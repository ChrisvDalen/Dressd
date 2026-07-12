import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Garment, GarmentCategory } from '../../core/models';

const LABELS: Record<GarmentCategory, string> = {
  TOP: 'Top',
  BOTTOM: 'Bottom',
  SOCKS: 'Socks',
  LAYER: 'Layer',
  SHOES: 'Shoes',
  ACCESSORY: 'Accessory',
};

/**
 * One horizontal swipe row for a single category (SPEC.md CategorySwipeComponent
 * / flow B). Swiping (or the arrow buttons / arrow keys) moves to the next or
 * previous garment and emits it upward, where the canvas re-composites in real
 * time. Shows a clear empty state with a scan CTA when the category has no
 * items (acceptance criterion "Lege staat").
 */
@Component({
  selector: 'app-category-swipe',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="row">
      <div class="label">{{ label() }}</div>

      @if (items().length === 0) {
        <div class="empty">
          <span>No {{ label().toLowerCase() }} yet.</span>
          <a routerLink="/scan" class="cta">Scan one →</a>
        </div>
      } @else {
        <div
          class="track"
          (touchstart)="onTouchStart($event)"
          (touchend)="onTouchEnd($event)"
          tabindex="0"
          role="listbox"
          [attr.aria-label]="label() + ' selector'"
          (keydown.arrowleft)="prev()"
          (keydown.arrowright)="next()"
        >
          <button class="nav" (click)="prev()" [disabled]="items().length < 2" aria-label="Previous">‹</button>

          <div class="item">
            <img [src]="current()!.imageUrl" [alt]="label()" />
            <span class="dot-row">
              @for (g of items(); track g.id; let i = $index) {
                <span class="dot" [class.active]="i === index()"></span>
              }
            </span>
          </div>

          <button class="nav" (click)="next()" [disabled]="items().length < 2" aria-label="Next">›</button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .row {
        display: grid;
        grid-template-columns: 70px 1fr;
        align-items: center;
        gap: 0.5rem;
        padding: 0.35rem 0;
      }
      .label {
        font-weight: 600;
        font-size: 0.85rem;
        color: #555;
      }
      .track {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        outline: none;
      }
      .nav {
        border: none;
        background: #ececec;
        border-radius: 50%;
        width: 34px;
        height: 34px;
        font-size: 1.3rem;
        cursor: pointer;
      }
      .nav:disabled {
        opacity: 0.35;
        cursor: default;
      }
      .item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
      }
      .item img {
        height: 64px;
        max-width: 100%;
        object-fit: contain;
      }
      .dot-row {
        display: flex;
        gap: 4px;
        margin-top: 4px;
      }
      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #ccc;
      }
      .dot.active {
        background: #6a4cff;
      }
      .empty {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: #999;
        font-size: 0.85rem;
      }
      .cta {
        color: #6a4cff;
        text-decoration: none;
        font-weight: 600;
      }
    `,
  ],
})
export class CategorySwipeComponent {
  readonly category = input.required<GarmentCategory>();
  readonly items = input<Garment[]>([]);
  readonly selectionChange = output<Garment | null>();

  protected readonly index = signal(0);
  protected readonly label = computed(() => LABELS[this.category()]);
  protected readonly current = computed<Garment | null>(() => this.items()[this.index()] ?? null);

  private touchStartX = 0;

  constructor() {
    // Keep the index in range whenever the item list changes.
    effect(() => {
      const len = this.items().length;
      if (this.index() >= len) {
        this.index.set(0);
      }
    });
    // Emit the current selection whenever it changes.
    effect(() => this.selectionChange.emit(this.current()));
  }

  next(): void {
    const len = this.items().length;
    if (len > 0) this.index.set((this.index() + 1) % len);
  }

  prev(): void {
    const len = this.items().length;
    if (len > 0) this.index.set((this.index() - 1 + len) % len);
  }

  onTouchStart(e: TouchEvent): void {
    this.touchStartX = e.changedTouches[0].clientX;
  }

  onTouchEnd(e: TouchEvent): void {
    const dx = e.changedTouches[0].clientX - this.touchStartX;
    if (Math.abs(dx) < 30) return;
    if (dx < 0) this.next();
    else this.prev();
  }
}
