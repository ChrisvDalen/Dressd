import { computed, inject } from '@angular/core';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { GarmentService, WardrobeFilter } from './garment.service';
import {
  CreateGarmentRequest,
  Garment,
  GarmentCategory,
  UpdateGarmentRequest,
} from './models';

/** Matches the server's maximum page size (com.dressd.common.web.PageRequests). */
const MAX_PAGE_SIZE = 200;

/**
 * Ceiling on how many pages the builder cache will walk. A wardrobe this large is
 * well past the point where the swipe UI needs a different design, and the cap
 * stops a paging bug from becoming an infinite request loop.
 */
const MAX_CACHED_PAGES = 20;

interface WardrobeState {
  garments: Garment[];
  filter: WardrobeFilter;
  loading: boolean;
  error: string | null;
  /** True when the wardrobe was larger than MAX_CACHED_PAGES could hold. */
  truncated: boolean;
}

const initialState: WardrobeState = {
  garments: [],
  filter: {},
  loading: false,
  error: null,
  truncated: false,
};

/**
 * NgRx Signal Store holding the wardrobe collection + active filters
 * (SPEC.md section 3, frontend). The whole garment collection is cached here so
 * the outfit builder can swipe between items with zero network latency
 * (SPEC.md flow B, step 4).
 */
export const WardrobeStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ garments }) => ({
    count: computed(() => garments().length),
    byCategory: computed(() => {
      const map = new Map<GarmentCategory, Garment[]>();
      for (const g of garments()) {
        const bucket = map.get(g.category) ?? [];
        bucket.push(g);
        map.set(g.category, bucket);
      }
      return map;
    }),
  })),
  withMethods((store, garmentApi = inject(GarmentService)) => ({
    /** Load the full collection (unfiltered) — used to warm the builder cache. */
    async loadAll(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try {
        const garments: Garment[] = [];
        let pageIndex = 0;
        let truncated = false;

        for (;;) {
          const page = await firstValueFrom(
            garmentApi.list({}, { page: pageIndex, size: MAX_PAGE_SIZE }),
          );
          garments.push(...page.content);
          if (page.last) break;
          if (++pageIndex >= MAX_CACHED_PAGES) {
            truncated = true;
            break;
          }
        }

        patchState(store, { garments, loading: false, truncated });
      } catch (e) {
        patchState(store, { loading: false, error: describe(e) });
      }
    },

    /** Apply a filter and reload the grid from the server (flow C). */
    async applyFilter(filter: WardrobeFilter): Promise<void> {
      patchState(store, { filter, loading: true, error: null });
      try {
        const page = await firstValueFrom(garmentApi.list(filter, { size: MAX_PAGE_SIZE }));
        patchState(store, {
          garments: page.content,
          loading: false,
          truncated: !page.last,
        });
      } catch (e) {
        patchState(store, { loading: false, error: describe(e) });
      }
    },

    async add(request: CreateGarmentRequest): Promise<Garment | null> {
      try {
        const created = await firstValueFrom(garmentApi.create(request));
        patchState(store, { garments: [created, ...store.garments()], error: null });
        return created;
      } catch (e) {
        patchState(store, { error: describe(e) });
        return null;
      }
    },

    async edit(id: string, request: UpdateGarmentRequest): Promise<boolean> {
      try {
        const updated = await firstValueFrom(garmentApi.update(id, request));
        patchState(store, {
          garments: store.garments().map((g) => (g.id === id ? updated : g)),
          error: null,
        });
        return true;
      } catch (e) {
        patchState(store, { error: describe(e) });
        return false;
      }
    },

    async remove(id: string): Promise<boolean> {
      try {
        await firstValueFrom(garmentApi.remove(id));
        patchState(store, {
          garments: store.garments().filter((g) => g.id !== id),
          error: null,
        });
        return true;
      } catch (e) {
        patchState(store, { error: describe(e) });
        return false;
      }
    },

    clearError(): void {
      patchState(store, { error: null });
    },

    garmentsInCategory(category: GarmentCategory): Garment[] {
      return store.garments().filter((g) => g.category === category);
    },
  })),
);

/**
 * Turns a failed request into something worth showing a user. Prefers the
 * server's own `ApiError.message` over Angular's generic HTTP wrapper text.
 */
function describe(error: unknown): string {
  if (error && typeof error === 'object') {
    const apiMessage = (error as { error?: { message?: unknown } }).error?.message;
    if (typeof apiMessage === 'string' && apiMessage.length > 0) {
      return apiMessage;
    }
    if ((error as { status?: unknown }).status === 0) {
      return 'Could not reach the server. Is the backend running?';
    }
    const message = (error as { message?: unknown }).message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
  }
  return 'Something went wrong. Is the backend running?';
}
