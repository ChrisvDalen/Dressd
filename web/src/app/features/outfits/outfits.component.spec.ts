import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { Garment, GarmentCategory, Outfit, Page } from '../../core/models';
import { OutfitService } from '../../core/outfit.service';
import { WardrobeStore } from '../../core/wardrobe.store';
import { OutfitsComponent } from './outfits.component';

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

function outfit(id: string, garmentIds: string[]): Outfit {
  return {
    id,
    ownerId: 'owner-1',
    avatarId: 'avatar-1',
    name: `outfit ${id}`,
    garmentLayers: garmentIds.map((garmentId, i) => ({
      garmentId,
      category: 'TOP' as GarmentCategory,
      zIndex: i,
      offsetX: 0,
      offsetY: 0,
      scale: 1,
    })),
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

function pageOf(outfits: Outfit[]): Page<Outfit> {
  return {
    content: outfits,
    page: 0,
    size: 200,
    totalElements: outfits.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

class FakeOutfitService implements Partial<OutfitService> {
  outfits: Outfit[] = [];
  listError: unknown = null;
  removeError: unknown = null;
  removed: string[] = [];

  list(): Observable<Page<Outfit>> {
    if (this.listError) return throwError(() => this.listError);
    return of(pageOf(this.outfits));
  }

  remove(id: string): Observable<void> {
    if (this.removeError) return throwError(() => this.removeError);
    this.removed.push(id);
    return of(undefined);
  }
}

/** Stands in for the store so the gallery's garment index is controllable. */
class FakeWardrobeStore {
  private readonly value: Garment[];

  constructor(garments: Garment[]) {
    this.value = garments;
  }

  garments = () => this.value;
  loadAll = async () => undefined;
}

/**
 * Lets the component's async `ngOnInit` / click handlers settle. The app is
 * zoneless, so Angular does not track plain promises for us; a macrotask tick
 * runs after every pending microtask has drained.
 */
async function settle(fixture: { detectChanges(): void }) {
  await new Promise((resolve) => setTimeout(resolve, 0));
  fixture.detectChanges();
}

async function render(outfits: Outfit[], garments: Garment[], api = new FakeOutfitService()) {
  api.outfits = outfits;
  TestBed.configureTestingModule({
    imports: [OutfitsComponent],
    providers: [
      provideRouter([]),
      { provide: OutfitService, useValue: api },
      { provide: WardrobeStore, useValue: new FakeWardrobeStore(garments) },
    ],
  });
  const fixture = TestBed.createComponent(OutfitsComponent);
  fixture.detectChanges();
  await settle(fixture);
  return { fixture, api, element: fixture.nativeElement as HTMLElement };
}

describe('OutfitsComponent', () => {
  it('renders saved outfits', async () => {
    const { element } = await render([outfit('o1', ['g1'])], [garment('g1')]);

    expect(element.textContent).toContain('outfit o1');
    expect(element.querySelectorAll('.card')).toHaveLength(1);
  });

  it('shows the empty state when there are no outfits', async () => {
    const { element } = await render([], []);

    expect(element.textContent).toContain('No outfits yet');
  });

  it('notes layers whose garment is no longer in the wardrobe', async () => {
    // Two layers saved, but only one garment still exists.
    const { element } = await render([outfit('o1', ['g1', 'deleted-g2'])], [garment('g1')]);

    expect(element.textContent).toContain('1 item(s) no longer in your wardrobe');
  });

  it('says nothing when every layer still resolves', async () => {
    const { element } = await render([outfit('o1', ['g1', 'g2'])], [garment('g1'), garment('g2')]);

    expect(element.textContent).not.toContain('no longer in your wardrobe');
  });

  it('reports a failed load instead of hanging on the spinner', async () => {
    const api = new FakeOutfitService();
    api.listError = new Error('offline');

    const { element } = await render([], [], api);

    expect(element.textContent).toContain('Could not load your outfits');
    expect(element.textContent).not.toContain('Loading…');
  });

  it('removes an outfit from the list once deleted', async () => {
    const { element, api, fixture } = await render(
      [outfit('o1', ['g1']), outfit('o2', ['g1'])],
      [garment('g1')],
    );
    expect(element.querySelectorAll('.card')).toHaveLength(2);

    (element.querySelector('.del') as HTMLButtonElement).click();
    await settle(fixture);

    expect(api.removed).toEqual(['o1']);
    expect(element.querySelectorAll('.card')).toHaveLength(1);
  });

  it('keeps the outfit and reports the failure when deletion fails', async () => {
    const api = new FakeOutfitService();
    api.removeError = new Error('nope');
    const { element, fixture } = await render([outfit('o1', ['g1'])], [garment('g1')], api);

    (element.querySelector('.del') as HTMLButtonElement).click();
    await settle(fixture);

    expect(element.textContent).toContain('Could not delete that outfit');
    expect(element.querySelectorAll('.card')).toHaveLength(1);
  });
});
