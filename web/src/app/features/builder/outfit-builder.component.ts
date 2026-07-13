import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AvatarService } from '../../core/avatar.service';
import {
  Avatar,
  BodyType,
  BodyTypePreset,
  Garment,
  GarmentCategory,
  GarmentLayer,
  Proportions,
  SWIPE_CATEGORIES,
} from '../../core/models';
import { OutfitService } from '../../core/outfit.service';
import { WardrobeStore } from '../../core/wardrobe.store';
import { AvatarCanvasComponent, PlacedGarment } from './avatar-canvas.component';
import { CategorySwipeComponent } from './category-swipe.component';

@Component({
  selector: 'app-outfit-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, AvatarCanvasComponent, CategorySwipeComponent],
  template: `
    <section class="builder">
      <div class="stage">
        <app-avatar-canvas [avatar]="previewAvatar()" [layers]="placedLayers()" />

        <div class="body-types">
          @for (preset of bodyTypes(); track preset.bodyType) {
            <button
              class="chip"
              [class.active]="preset.bodyType === selectedBodyType()"
              (click)="selectedBodyType.set(preset.bodyType)"
            >
              {{ preset.label }}
            </button>
          }
        </div>
      </div>

      <div class="controls">
        <h2>Style an outfit</h2>
        @if (store.count() === 0) {
          <p class="hint">Your wardrobe is empty — scan a few items to start composing.</p>
        }

        @for (category of categories; track category) {
          <app-category-swipe
            [category]="category"
            [items]="itemsFor(category)"
            (selectionChange)="onSelection(category, $event)"
          />
        }

        <div class="save">
          <input
            type="text"
            [(ngModel)]="outfitName"
            placeholder="Outfit name (optional)"
            aria-label="Outfit name"
          />
          <button (click)="saveOutfit()" [disabled]="saving() || !hasSelection()">
            {{ saving() ? 'Saving…' : 'Save outfit' }}
          </button>
        </div>
        @if (savedMessage()) {
          <p class="saved">{{ savedMessage() }}</p>
        }
      </div>
    </section>
  `,
  styles: [
    `
      .builder {
        display: grid;
        grid-template-columns: minmax(240px, 360px) 1fr;
        gap: 2rem;
        align-items: start;
      }
      @media (max-width: 760px) {
        .builder {
          grid-template-columns: 1fr;
        }
      }
      .stage {
        position: sticky;
        top: 1rem;
        background: #faf9ff;
        border-radius: 16px;
        padding: 1rem;
      }
      .body-types {
        display: flex;
        gap: 0.5rem;
        justify-content: center;
        margin-top: 0.75rem;
        flex-wrap: wrap;
      }
      .chip {
        border: 1px solid #d5cdff;
        background: #fff;
        color: #4a3bbf;
        border-radius: 999px;
        padding: 0.3rem 0.8rem;
        cursor: pointer;
        font-size: 0.8rem;
      }
      .chip.active {
        background: #6a4cff;
        color: #fff;
        border-color: #6a4cff;
      }
      .controls h2 {
        margin-top: 0;
      }
      .hint {
        color: #888;
      }
      .save {
        display: flex;
        gap: 0.5rem;
        margin-top: 1rem;
      }
      .save input {
        flex: 1;
        padding: 0.5rem 0.7rem;
        border: 1px solid #ddd;
        border-radius: 8px;
      }
      .save button {
        background: #6a4cff;
        color: #fff;
        border: none;
        border-radius: 8px;
        padding: 0.5rem 1rem;
        cursor: pointer;
      }
      .save button:disabled {
        opacity: 0.5;
        cursor: default;
      }
      .saved {
        color: #1a9d5a;
        font-weight: 600;
      }
    `,
  ],
})
export class OutfitBuilderComponent implements OnInit {
  protected readonly store = inject(WardrobeStore);
  private readonly avatarApi = inject(AvatarService);
  private readonly outfitApi = inject(OutfitService);

  protected readonly categories = SWIPE_CATEGORIES;
  protected readonly bodyTypes = signal<BodyTypePreset[]>([]);
  protected readonly selectedBodyType = signal<BodyType>('AVERAGE');
  protected readonly selections = signal<Partial<Record<GarmentCategory, Garment | null>>>({});
  protected readonly saving = signal(false);
  protected readonly savedMessage = signal<string | null>(null);
  protected outfitName = '';

  private avatarsByBodyType = new Map<BodyType, Avatar>();

  protected readonly previewAvatar = computed<Avatar | null>(() => {
    const preset = this.bodyTypes().find((p) => p.bodyType === this.selectedBodyType());
    const proportions: Proportions = preset?.proportions ?? {
      height: 1,
      shoulderWidth: 1,
      hipWidth: 1,
    };
    return {
      id: 'preview',
      ownerId: 'preview',
      name: preset?.label ?? 'Average',
      bodyType: this.selectedBodyType(),
      proportions,
    };
  });

  protected readonly placedLayers = computed<PlacedGarment[]>(() => {
    const sel = this.selections();
    return this.categories
      .filter((c) => sel[c])
      .map((c) => ({ category: c, garment: sel[c] as Garment }));
  });

  protected readonly hasSelection = computed(() => this.placedLayers().length > 0);

  async ngOnInit(): Promise<void> {
    await this.store.loadAll();
    try {
      const [presets, avatars] = await Promise.all([
        firstValueFrom(this.avatarApi.bodyTypes()),
        firstValueFrom(this.avatarApi.list()),
      ]);
      this.bodyTypes.set(presets.filter((p) => p.bodyType !== 'CUSTOM'));
      for (const a of avatars) {
        this.avatarsByBodyType.set(a.bodyType, a);
      }
    } catch {
      // Body types are static; if the avatar service is down we still render.
      this.bodyTypes.set([
        { bodyType: 'SLIM', label: 'Slim', proportions: { height: 1.02, shoulderWidth: 0.9, hipWidth: 0.88 } },
        { bodyType: 'AVERAGE', label: 'Average', proportions: { height: 1, shoulderWidth: 1, hipWidth: 1 } },
        { bodyType: 'CURVY', label: 'Curvy', proportions: { height: 0.98, shoulderWidth: 1.05, hipWidth: 1.15 } },
      ]);
    }
  }

  protected itemsFor(category: GarmentCategory): Garment[] {
    return this.store.garmentsInCategory(category);
  }

  protected onSelection(category: GarmentCategory, garment: Garment | null): void {
    this.selections.update((s) => ({ ...s, [category]: garment }));
    this.savedMessage.set(null);
  }

  protected async saveOutfit(): Promise<void> {
    this.saving.set(true);
    this.savedMessage.set(null);
    try {
      const avatar = await this.ensureAvatar(this.selectedBodyType());
      const layers: GarmentLayer[] = this.placedLayers().map((l, i) => ({
        garmentId: l.garment.id,
        category: l.category,
        zIndex: i,
        offsetX: 0,
        offsetY: 0,
        scale: 1,
      }));
      const outfit = await firstValueFrom(
        this.outfitApi.create({
          avatarId: avatar.id,
          name: this.outfitName.trim() || null,
          garmentLayers: layers,
        }),
      );
      this.savedMessage.set(`Saved "${outfit.name ?? 'outfit'}" ✓`);
      this.outfitName = '';
    } catch {
      this.savedMessage.set('Could not save outfit. Is the backend running?');
    } finally {
      this.saving.set(false);
    }
  }

  /** Reuse an existing avatar for the chosen body type, else create one. */
  private async ensureAvatar(bodyType: BodyType): Promise<Avatar> {
    const existing = this.avatarsByBodyType.get(bodyType);
    if (existing) return existing;
    const created = await firstValueFrom(
      this.avatarApi.create({ bodyType, name: bodyType, proportions: null }),
    );
    this.avatarsByBodyType.set(bodyType, created);
    return created;
  }
}
