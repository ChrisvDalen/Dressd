import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Avatar, Garment, GarmentCategory } from '../../core/models';

/** A garment currently placed on the avatar. */
export interface PlacedGarment {
  category: GarmentCategory;
  garment: Garment;
}

interface RenderBand {
  x: number;
  y: number;
  width: number;
  height: number;
  z: number;
}

// Base placement bands within the 200x400 viewBox, keyed by category.
const BANDS: Record<GarmentCategory, RenderBand> = {
  LAYER: { x: 34, y: 96, width: 132, height: 150, z: 4 },
  TOP: { x: 52, y: 100, width: 96, height: 130, z: 3 },
  BOTTOM: { x: 58, y: 210, width: 84, height: 150, z: 2 },
  SHOES: { x: 60, y: 356, width: 80, height: 36, z: 1 },
  SOCKS: { x: 66, y: 330, width: 68, height: 34, z: 0 },
  ACCESSORY: { x: 78, y: 40, width: 44, height: 44, z: 5 },
};

/**
 * Renders the 2D silhouette plus the currently selected garment layers
 * (SPEC.md AvatarCanvasComponent). Pure SVG, driven entirely by inputs — the
 * swipe component just changes which garments are passed in and the layers
 * update in place (flow B, real-time client-side compositing).
 */
@Component({
  selector: 'app-avatar-canvas',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 200 400" class="avatar" role="img" aria-label="Outfit preview on silhouette">
      <!-- Silhouette -->
      <g [attr.transform]="silhouetteTransform()">
        <ellipse cx="100" cy="46" rx="20" ry="24" class="skin" />
        <path [attr.d]="bodyPath()" class="skin" />
      </g>

      <!-- Garment layers, drawn back-to-front by z-index -->
      @for (layer of orderedLayers(); track layer.garment.id) {
        <image
          [attr.href]="layer.garment.imageUrl"
          [attr.x]="layer.band.x"
          [attr.y]="layer.band.y"
          [attr.width]="layer.band.width"
          [attr.height]="layer.band.height"
          preserveAspectRatio="xMidYMid meet"
        />
      }
    </svg>
  `,
  styles: [
    `
      .avatar {
        width: 100%;
        max-width: 320px;
        height: auto;
        display: block;
        margin: 0 auto;
      }
      .skin {
        fill: #d9c2a6;
        stroke: #b89a76;
        stroke-width: 1;
      }
    `,
  ],
})
export class AvatarCanvasComponent {
  readonly avatar = input<Avatar | null>(null);
  readonly layers = input<PlacedGarment[]>([]);

  protected readonly orderedLayers = computed(() =>
    [...this.layers()]
      .map((l) => ({ ...l, band: BANDS[l.category] }))
      .sort((a, b) => a.band.z - b.band.z),
  );

  protected readonly silhouetteTransform = computed(() => {
    const p = this.avatar()?.proportions;
    if (!p) return '';
    // Scale horizontally around the centre line (x=100) to reflect proportions.
    const scaleX = (p.shoulderWidth + p.hipWidth) / 2;
    const scaleY = p.height;
    return `translate(100 0) scale(${scaleX.toFixed(3)} ${scaleY.toFixed(3)}) translate(-100 0)`;
  });

  protected readonly bodyPath = computed(() => {
    const p = this.avatar()?.proportions ?? { shoulderWidth: 1, hipWidth: 1, height: 1 };
    const shoulder = 32 * p.shoulderWidth;
    const hip = 28 * p.hipWidth;
    // A simple torso+legs outline centred on x=100.
    return [
      `M ${100 - shoulder} 96`,
      `Q ${100 - shoulder - 6} 150 ${100 - hip} 210`,
      `L ${100 - hip} 250`,
      `L ${100 - 8} 250`,
      `L ${100 - 10} 366`,
      `L ${100 - 26} 366`,
      `L ${100 - hip - 2} 252`,
      `L ${100 - hip - 6} 210`,
      `Q ${100 - shoulder - 10} 150 ${100 - shoulder - 2} 96`,
      // right side mirrored
      `L ${100 + shoulder + 2} 96`,
      `Q ${100 + shoulder + 10} 150 ${100 + hip + 6} 210`,
      `L ${100 + hip + 2} 252`,
      `L ${100 + 26} 366`,
      `L ${100 + 10} 366`,
      `L ${100 + 8} 250`,
      `L ${100 + hip} 250`,
      `L ${100 + hip} 210`,
      `Q ${100 + shoulder + 6} 150 ${100 + shoulder} 96`,
      'Z',
    ].join(' ');
  });
}
