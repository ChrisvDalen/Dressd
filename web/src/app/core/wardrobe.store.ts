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

interface WardrobeState {
  garments: Garment[];
  filter: WardrobeFilter;
  loading: boolean;
  error: string | null;
}

const initialState: WardrobeState = {
  garments: [],
  filter: {},
  loading: false,
  error: null,
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
        const garments = await firstValueFrom(garmentApi.list());
        patchState(store, { garments, loading: false });
      } catch (e) {
        patchState(store, { loading: false, error: describe(e) });
      }
    },

    /** Apply a filter and reload the grid from the server (flow C). */
    async applyFilter(filter: WardrobeFilter): Promise<void> {
      patchState(store, { filter, loading: true, error: null });
      try {
        const garments = await firstValueFrom(garmentApi.list(filter));
        patchState(store, { garments, loading: false });
      } catch (e) {
        patchState(store, { loading: false, error: describe(e) });
      }
    },

    async add(request: CreateGarmentRequest): Promise<Garment | null> {
      try {
        const created = await firstValueFrom(garmentApi.create(request));
        patchState(store, { garments: [created, ...store.garments()] });
        return created;
      } catch (e) {
        patchState(store, { error: describe(e) });
        return null;
      }
    },

    async edit(id: string, request: UpdateGarmentRequest): Promise<void> {
      const updated = await firstValueFrom(garmentApi.update(id, request));
      patchState(store, {
        garments: store.garments().map((g) => (g.id === id ? updated : g)),
      });
    },

    async remove(id: string): Promise<void> {
      await firstValueFrom(garmentApi.remove(id));
      patchState(store, { garments: store.garments().filter((g) => g.id !== id) });
    },

    garmentsInCategory(category: GarmentCategory): Garment[] {
      return store.garments().filter((g) => g.category === category);
    },
  })),
);

function describe(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: unknown }).message);
  }
  return 'Something went wrong. Is the backend running?';
}
