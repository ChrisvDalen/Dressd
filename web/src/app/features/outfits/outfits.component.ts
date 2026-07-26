import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Garment, Outfit } from '../../core/models';
import { OutfitService } from '../../core/outfit.service';
import { WardrobeStore } from '../../core/wardrobe.store';
import { AvatarCanvasComponent, PlacedGarment } from '../builder/avatar-canvas.component';

/** Saved outfits gallery (SPEC.md: outfit opslaan en later terug opvragen). */
@Component({
  selector: 'app-outfits',
  standalone: true,
  imports: [CommonModule, RouterLink, AvatarCanvasComponent],
  template: `
    <section>
      <header class="head">
        <h2>Saved outfits</h2>
        <a routerLink="/builder" class="cta">+ New outfit</a>
      </header>

      @if (error()) {
        <p class="err">{{ error() }}</p>
      }

      @if (loading()) {
        <p class="muted">Loading…</p>
      } @else if (outfits().length === 0) {
        <div class="empty">
          <p>No outfits yet. Compose one in the builder.</p>
          <a routerLink="/builder" class="cta">Open builder</a>
        </div>
      } @else {
        <div class="grid">
          @for (o of outfits(); track o.id) {
            <figure class="card">
              <app-avatar-canvas [avatar]="null" [layers]="layersFor(o)" />
              <figcaption>
                <span>{{ o.name || 'Untitled outfit' }}</span>
                <button class="del" (click)="remove(o)" aria-label="Delete">🗑</button>
              </figcaption>
              @if (missingCount(o) > 0) {
                <p class="missing">
                  {{ missingCount(o) }} item(s) no longer in your wardrobe
                </p>
              }
            </figure>
          }
        </div>
      }
    </section>
  `,
  styles: [
    `
      .head {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      .cta {
        background: #6a4cff;
        color: #fff;
        padding: 0.45rem 0.9rem;
        border-radius: 8px;
        text-decoration: none;
        font-weight: 600;
      }
      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
        gap: 1rem;
        margin-top: 1rem;
      }
      .card {
        margin: 0;
        border: 1px solid #eee;
        border-radius: 12px;
        padding: 0.5rem;
        background: #faf9ff;
      }
      figcaption {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 0.5rem;
        font-weight: 600;
      }
      .del {
        border: none;
        background: transparent;
        cursor: pointer;
      }
      .empty {
        text-align: center;
        padding: 3rem 0;
        color: #888;
      }
      .muted {
        color: #999;
      }
      .err {
        color: #c0392b;
      }
      .missing {
        margin: 0.25rem 0 0;
        font-size: 0.75rem;
        color: #a0752b;
      }
    `,
  ],
})
export class OutfitsComponent implements OnInit {
  private readonly outfitApi = inject(OutfitService);
  private readonly store = inject(WardrobeStore);

  protected readonly outfits = signal<Outfit[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  private garmentIndex = computed(() => {
    const map = new Map<string, Garment>();
    for (const g of this.store.garments()) map.set(g.id, g);
    return map;
  });

  async ngOnInit(): Promise<void> {
    await this.store.loadAll();
    try {
      const page = await firstValueFrom(this.outfitApi.list({ size: 200 }));
      this.outfits.set(page.content);
    } catch {
      this.error.set('Could not load your outfits. Is the backend running?');
    } finally {
      this.loading.set(false);
    }
  }

  protected layersFor(outfit: Outfit): PlacedGarment[] {
    const index = this.garmentIndex();
    return outfit.garmentLayers
      .map((l) => {
        const garment = index.get(l.garmentId);
        return garment ? { category: l.category, garment } : null;
      })
      .filter((x): x is PlacedGarment => x !== null);
  }

  /**
   * Layers whose garment has since been deleted. Garments and outfits live in
   * separate services with no cross-service foreign key (docs/ADR.md, ADR-008),
   * so a saved outfit can outlive an item — the gallery says so rather than
   * quietly rendering a gap.
   */
  protected missingCount(outfit: Outfit): number {
    return outfit.garmentLayers.length - this.layersFor(outfit).length;
  }

  protected async remove(outfit: Outfit): Promise<void> {
    try {
      await firstValueFrom(this.outfitApi.remove(outfit.id));
      this.outfits.update((list) => list.filter((o) => o.id !== outfit.id));
    } catch {
      this.error.set('Could not delete that outfit. Please try again.');
    }
  }
}
