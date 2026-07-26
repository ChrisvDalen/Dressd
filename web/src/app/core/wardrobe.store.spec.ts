import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { GarmentService, PageRequest, WardrobeFilter } from './garment.service';
import { Garment, GarmentCategory, Page } from './models';
import { WardrobeStore } from './wardrobe.store';

function garment(id: string, category: GarmentCategory = 'TOP'): Garment {
  return {
    id,
    ownerId: 'owner-1',
    category,
    imageUrl: `/media/garments/owner-1/${id}.png`,
    colorTag: '#3355ff',
    pattern: 'solid',
    season: null,
    anchorPoints: { shoulderY: 0.2, waistY: 0.45, hemY: 0.7, widthScale: 1 },
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

function pageOf(content: Garment[], last = true, index = 0): Page<Garment> {
  return {
    content,
    page: index,
    size: 200,
    totalElements: content.length,
    totalPages: last ? index + 1 : index + 2,
    first: index === 0,
    last,
  };
}

/** Records the calls the store makes so paging behaviour can be asserted. */
class FakeGarmentService implements Partial<GarmentService> {
  listCalls: Array<{ filter: WardrobeFilter; page: PageRequest }> = [];
  pages: Array<Page<Garment>> = [pageOf([])];
  listError: unknown = null;
  updateError: unknown = null;
  removeError: unknown = null;
  createError: unknown = null;

  list(filter: WardrobeFilter = {}, page: PageRequest = {}): Observable<Page<Garment>> {
    this.listCalls.push({ filter, page });
    if (this.listError) return throwError(() => this.listError);
    const requested = page.page ?? 0;
    return of(this.pages[requested] ?? pageOf([]));
  }

  create(): Observable<Garment> {
    if (this.createError) return throwError(() => this.createError);
    return of(garment('new'));
  }

  update(id: string): Observable<Garment> {
    if (this.updateError) return throwError(() => this.updateError);
    return of({ ...garment(id), category: 'LAYER' as GarmentCategory });
  }

  remove(): Observable<void> {
    if (this.removeError) return throwError(() => this.removeError);
    return of(undefined);
  }
}

describe('WardrobeStore', () => {
  let api: FakeGarmentService;
  let store: InstanceType<typeof WardrobeStore>;

  beforeEach(() => {
    api = new FakeGarmentService();
    TestBed.configureTestingModule({
      providers: [{ provide: GarmentService, useValue: api }],
    });
    store = TestBed.inject(WardrobeStore);
  });

  describe('loadAll', () => {
    it('requests the maximum page size', async () => {
      api.pages = [pageOf([garment('g1')])];

      await store.loadAll();

      expect(api.listCalls[0].page).toEqual({ page: 0, size: 200 });
      expect(store.garments()).toHaveLength(1);
      expect(store.loading()).toBe(false);
    });

    it('follows pages until the last one and concatenates them', async () => {
      api.pages = [
        pageOf([garment('g1'), garment('g2')], false, 0),
        pageOf([garment('g3')], true, 1),
      ];

      await store.loadAll();

      expect(api.listCalls).toHaveLength(2);
      expect(store.garments().map((g) => g.id)).toEqual(['g1', 'g2', 'g3']);
      expect(store.truncated()).toBe(false);
    });

    it('stops after the page cap and flags the result as truncated', async () => {
      // Every page claims there is another one, so only the cap ends the loop.
      api.pages = Array.from({ length: 40 }, (_, i) => pageOf([garment(`g${i}`)], false, i));

      await store.loadAll();

      expect(api.listCalls).toHaveLength(20);
      expect(store.truncated()).toBe(true);
    });

    it('records an error instead of throwing', async () => {
      api.listError = new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' });

      await expect(store.loadAll()).resolves.toBeUndefined();

      expect(store.error()).toContain('Could not reach the server');
      expect(store.loading()).toBe(false);
    });
  });

  describe('applyFilter', () => {
    it('passes the filter through and stores it', async () => {
      await store.applyFilter({ category: 'SHOES' });

      expect(api.listCalls[0].filter).toEqual({ category: 'SHOES' });
      expect(store.filter()).toEqual({ category: 'SHOES' });
    });

    it('surfaces the API error message when the server sends one', async () => {
      api.listError = new HttpErrorResponse({
        status: 400,
        error: { message: 'Invalid X-Owner-Id header: not a UUID' },
      });

      await store.applyFilter({});

      expect(store.error()).toBe('Invalid X-Owner-Id header: not a UUID');
    });
  });

  describe('mutations', () => {
    it('prepends a created garment', async () => {
      api.pages = [pageOf([garment('g1')])];
      await store.loadAll();

      const created = await store.add({ category: 'TOP', imageUrl: '/media/pending/o/x.png' });

      expect(created?.id).toBe('new');
      expect(store.garments().map((g) => g.id)).toEqual(['new', 'g1']);
    });

    it('replaces the edited garment in place', async () => {
      api.pages = [pageOf([garment('g1'), garment('g2')])];
      await store.loadAll();

      await expect(store.edit('g1', { category: 'LAYER' })).resolves.toBe(true);

      expect(store.garments()[0].category).toBe('LAYER');
      expect(store.garments()[1].category).toBe('TOP');
    });

    it('reports a failed edit through state rather than rejecting', async () => {
      api.pages = [pageOf([garment('g1')])];
      await store.loadAll();
      api.updateError = new HttpErrorResponse({ status: 500, error: { message: 'boom' } });

      await expect(store.edit('g1', { category: 'LAYER' })).resolves.toBe(false);

      expect(store.error()).toBe('boom');
      // The optimistic update is not applied on failure.
      expect(store.garments()[0].category).toBe('TOP');
    });

    it('drops the removed garment', async () => {
      api.pages = [pageOf([garment('g1'), garment('g2')])];
      await store.loadAll();

      await expect(store.remove('g1')).resolves.toBe(true);

      expect(store.garments().map((g) => g.id)).toEqual(['g2']);
    });

    it('reports a failed removal through state and keeps the garment', async () => {
      api.pages = [pageOf([garment('g1')])];
      await store.loadAll();
      api.removeError = new HttpErrorResponse({ status: 409, error: { message: 'nope' } });

      await expect(store.remove('g1')).resolves.toBe(false);

      expect(store.error()).toBe('nope');
      expect(store.garments()).toHaveLength(1);
    });

    it('clears a previous error on the next success', async () => {
      api.pages = [pageOf([garment('g1')])];
      await store.loadAll();
      api.removeError = new HttpErrorResponse({ status: 500, error: { message: 'nope' } });
      await store.remove('g1');
      expect(store.error()).toBe('nope');

      api.removeError = null;
      await store.remove('g1');

      expect(store.error()).toBeNull();
    });
  });

  describe('derived state', () => {
    it('buckets garments by category and counts them', async () => {
      api.pages = [
        pageOf([garment('g1', 'TOP'), garment('g2', 'SHOES'), garment('g3', 'TOP')]),
      ];

      await store.loadAll();

      expect(store.count()).toBe(3);
      expect(store.byCategory().get('TOP')?.map((g) => g.id)).toEqual(['g1', 'g3']);
      expect(store.byCategory().get('SHOES')).toHaveLength(1);
      expect(store.byCategory().get('BOTTOM')).toBeUndefined();
      expect(store.garmentsInCategory('TOP')).toHaveLength(2);
    });
  });
});
