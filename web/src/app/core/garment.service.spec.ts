import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GarmentService } from './garment.service';
import { Garment, Page } from './models';

function garment(id: string, overrides: Partial<Garment> = {}): Garment {
  return {
    id,
    ownerId: 'owner-1',
    category: 'TOP',
    imageUrl: `/media/garments/owner-1/${id}.png`,
    colorTag: '#3355ff',
    pattern: 'solid',
    season: null,
    anchorPoints: { shoulderY: 0.2, waistY: 0.45, hemY: 0.7, widthScale: 1 },
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

describe('GarmentService', () => {
  let service: GarmentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GarmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends no query parameters for an empty filter', () => {
    service.list().subscribe();

    const req = http.expectOne('/api/garments');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual([]);
    req.flush(page([]));
  });

  it('maps filters and paging onto query parameters', () => {
    service.list({ category: 'SHOES', color: '#33', season: 'WINTER' }, { page: 2, size: 50 })
      .subscribe();

    const req = http.expectOne((r) => r.url === '/api/garments');
    expect(req.request.params.get('category')).toBe('SHOES');
    expect(req.request.params.get('color')).toBe('#33');
    expect(req.request.params.get('season')).toBe('WINTER');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('50');
    req.flush(page([]));
  });

  it('omits blank filter values rather than sending empty parameters', () => {
    service.list({ category: null, color: '', season: null }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/garments');
    expect(req.request.params.keys()).toEqual([]);
    req.flush(page([]));
  });

  it('returns the page envelope untouched', () => {
    let received: Page<Garment> | undefined;
    service.list().subscribe((p) => (received = p));

    http.expectOne((r) => r.url === '/api/garments').flush(
      page([garment('g1')], { totalElements: 7, totalPages: 4, last: false }),
    );

    expect(received?.content).toHaveLength(1);
    expect(received?.totalElements).toBe(7);
    expect(received?.last).toBe(false);
  });

  it('posts the scan photo as multipart form data', () => {
    service.scan(new Blob(['bytes'], { type: 'image/png' }), 'shirt.png').subscribe();

    const req = http.expectOne('/api/scan');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeInstanceOf(FormData);
    req.flush({
      imageUrl: '/media/pending/owner-1/x.png',
      suggestedCategory: 'TOP',
      suggestedColorTag: '#3355ff',
      suggestedPattern: 'solid',
    });
  });

  it('patches only the changed fields on update', () => {
    service.update('g1', { category: 'LAYER' }).subscribe();

    const req = http.expectOne('/api/garments/g1');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ category: 'LAYER' });
    req.flush(garment('g1', { category: 'LAYER' }));
  });
});
