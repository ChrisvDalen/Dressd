import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Garment,
  GarmentCategory,
  Season,
  WEARABLE_CATEGORIES,
} from '../../core/models';
import { WardrobeStore } from '../../core/wardrobe.store';

/**
 * Wardrobe grid overview with category / colour / season filters
 * (SPEC.md WardrobeGrid / flow C). Items can be deleted or re-categorised.
 */
@Component({
  selector: 'app-wardrobe-grid',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section>
      <header class="head">
        <h2>Wardrobe <span class="count">{{ store.count() }}</span></h2>
        <a routerLink="/scan" class="scan-cta">+ Scan item</a>
      </header>

      <div class="filters">
        <select [(ngModel)]="category" (ngModelChange)="reload()">
          <option [ngValue]="null">All categories</option>
          @for (c of categories; track c) {
            <option [ngValue]="c">{{ c }}</option>
          }
        </select>
        <input
          type="text"
          [(ngModel)]="color"
          (ngModelChange)="reload()"
          placeholder="Colour (e.g. blue or #33)"
        />
        <select [(ngModel)]="season" (ngModelChange)="reload()">
          <option [ngValue]="null">Any season</option>
          <option value="SUMMER">Summer</option>
          <option value="WINTER">Winter</option>
          <option value="ALL">All year</option>
        </select>
      </div>

      @if (store.error()) {
        <p class="err">{{ store.error() }}</p>
      }

      @if (store.loading()) {
        <p class="muted">Loading…</p>
      } @else if (store.garments().length === 0) {
        <div class="empty">
          <p>No items match. Scan your first garment to get started.</p>
          <a routerLink="/scan" class="scan-cta">Scan a garment</a>
        </div>
      } @else {
        <div class="grid">
          @for (g of store.garments(); track g.id) {
            <figure class="card">
              <div class="thumb"><img [src]="g.imageUrl" [alt]="g.category" /></div>
              <figcaption>
                <select [ngModel]="g.category" (ngModelChange)="recategorise(g, $event)">
                  @for (c of categories; track c) {
                    <option [ngValue]="c">{{ c }}</option>
                  }
                </select>
                <span class="swatch" [style.background]="g.colorTag || '#ccc'" [title]="g.colorTag || ''"></span>
                <button class="del" (click)="remove(g)" aria-label="Delete">🗑</button>
              </figcaption>
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
      .count {
        background: #efeafe;
        color: #6a4cff;
        border-radius: 999px;
        padding: 0.1rem 0.6rem;
        font-size: 0.9rem;
        margin-left: 0.3rem;
      }
      .scan-cta {
        background: #6a4cff;
        color: #fff;
        padding: 0.45rem 0.9rem;
        border-radius: 8px;
        text-decoration: none;
        font-weight: 600;
      }
      .filters {
        display: flex;
        gap: 0.75rem;
        margin: 1rem 0;
        flex-wrap: wrap;
      }
      .filters select,
      .filters input {
        padding: 0.45rem 0.6rem;
        border: 1px solid #ddd;
        border-radius: 8px;
      }
      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
        gap: 1rem;
      }
      .card {
        margin: 0;
        border: 1px solid #eee;
        border-radius: 12px;
        overflow: hidden;
        background: #fff;
      }
      .thumb {
        aspect-ratio: 1;
        background: repeating-conic-gradient(#f4f4f4 0% 25%, #fff 0% 50%) 50% / 16px 16px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .thumb img {
        max-width: 90%;
        max-height: 90%;
        object-fit: contain;
      }
      figcaption {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        padding: 0.5rem;
      }
      figcaption select {
        flex: 1;
        border: 1px solid #eee;
        border-radius: 6px;
        padding: 0.2rem;
        font-size: 0.8rem;
      }
      .swatch {
        width: 18px;
        height: 18px;
        border-radius: 50%;
        border: 1px solid #ddd;
      }
      .del {
        border: none;
        background: transparent;
        cursor: pointer;
        font-size: 1rem;
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
    `,
  ],
})
export class WardrobeGridComponent implements OnInit {
  protected readonly store = inject(WardrobeStore);
  protected readonly categories = WEARABLE_CATEGORIES;

  protected category: GarmentCategory | null = null;
  protected color: string | null = null;
  protected season: Season | null = null;

  async ngOnInit(): Promise<void> {
    await this.reload();
  }

  protected async reload(): Promise<void> {
    await this.store.applyFilter({
      category: this.category,
      color: this.color,
      season: this.season,
    });
  }

  protected async recategorise(g: Garment, category: GarmentCategory): Promise<void> {
    if (category !== g.category) {
      await this.store.edit(g.id, { category });
    }
  }

  protected async remove(g: Garment): Promise<void> {
    await this.store.remove(g.id);
  }
}
