import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OutfitService } from './outfit.service';
import { GarmentLayer, Outfit, Page, SaveOutfitRequest } from './models';

function layer(garmentId: string, zIndex: number): GarmentLayer {
  return { garmentId, category: 'TOP', zIndex, offsetX: 0, offsetY: 0, scale: 1 };
}

function outfit(id: string, overrides: Partial<Outfit> = {}): Outfit {
  return {
    id,
    ownerId: 'owner-1',
    avatarId: 'a1',
    name: 'Look 1',
    garmentLayers: [layer('g1', 0)],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

function page<T>(content: T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return {
    content,
    page: 0,
    size: 200,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
    ...overrides,
  };
}

describe('OutfitService', () => {
  let service: OutfitService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OutfitService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends no paging parameters by default', () => {
    service.list().subscribe();

    const req = http.expectOne('/api/outfits');
    expect(req.request.params.keys()).toEqual([]);
    req.flush(page([]));
  });

  it('maps page and size onto query parameters', () => {
    service.list({ page: 1, size: 25 }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/outfits');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('25');
    req.flush(page([]));
  });

  it('fetches a single outfit by id', () => {
    let received: Outfit | undefined;
    service.get('o1').subscribe((o) => (received = o));

    const req = http.expectOne('/api/outfits/o1');
    expect(req.request.method).toBe('GET');
    req.flush(outfit('o1'));

    expect(received?.garmentLayers).toHaveLength(1);
  });

  it('creates an outfit with the full request body', () => {
    const request: SaveOutfitRequest = {
      avatarId: 'a1',
      name: 'Look',
      garmentLayers: [layer('g1', 0), layer('g2', 1)],
    };
    let received: Outfit | undefined;
    service.create(request).subscribe((o) => (received = o));

    const req = http.expectOne('/api/outfits');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(outfit('o1', { name: 'Look' }));

    expect(received?.name).toBe('Look');
  });

  it('updates the addressed outfit by id', () => {
    const request: SaveOutfitRequest = { avatarId: 'a1', garmentLayers: [layer('g9', 0)] };
    let received: Outfit | undefined;
    service.update('o1', request).subscribe((o) => (received = o));

    const req = http.expectOne('/api/outfits/o1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(outfit('o1'));

    expect(received?.id).toBe('o1');
  });

  it('deletes the addressed outfit by id', () => {
    let deleted = false;
    service.remove('o1').subscribe(() => (deleted = true));

    const req = http.expectOne('/api/outfits/o1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(deleted).toBe(true);
  });
});
