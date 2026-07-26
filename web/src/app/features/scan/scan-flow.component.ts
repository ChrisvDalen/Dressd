import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { GarmentService } from '../../core/garment.service';
import { GarmentCategory, ScanResult, Season, WEARABLE_CATEGORIES } from '../../core/models';
import { WardrobeStore } from '../../core/wardrobe.store';

type Phase = 'capture' | 'processing' | 'confirm';

/**
 * Camera scan flow (SPEC.md ScanFlowComponent / flow A). Capture a photo with
 * getUserMedia (or pick a file as a fallback), upload it for background removal
 * + suggestion, then let the user confirm or correct the metadata before it is
 * saved to the wardrobe.
 */
@Component({
  selector: 'app-scan-flow',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="scan">
      <h2>Scan a garment</h2>

      @if (phase() === 'capture') {
        <div class="camera">
          @if (cameraError()) {
            <p class="err">{{ cameraError() }}</p>
          }
          <video #video autoplay playsinline muted [class.hidden]="!streaming()"></video>
          <div class="actions">
            @if (streaming()) {
              <button class="primary" (click)="capture()">📷 Capture</button>
            } @else {
              <button (click)="startCamera()">Enable camera</button>
            }
            <label class="file">
              Upload photo
              <input type="file" accept="image/*" (change)="onFile($event)" hidden />
            </label>
          </div>
        </div>
      }

      @if (phase() === 'processing') {
        <div class="processing">
          <div class="spinner" aria-hidden="true"></div>
          <p>Removing background &amp; detecting category…</p>
        </div>
      }

      @if (phase() === 'confirm' && result()) {
        <div class="confirm">
          <div
            class="preview"
            [style.background]="'repeating-conic-gradient(#eee 0% 25%, #fff 0% 50%) 50% / 20px 20px'"
          >
            <img [src]="result()!.imageUrl" alt="Cut-out preview" />
          </div>
          <form (submit)="$event.preventDefault(); confirm()">
            <label>
              Category
              <select [(ngModel)]="category" name="category">
                @for (c of allCategories; track c) {
                  <option [value]="c">{{ c }}</option>
                }
              </select>
            </label>
            <label>
              Colour
              <span class="color-row">
                <input type="color" [(ngModel)]="colorTag" name="colorPick" />
                <input type="text" [(ngModel)]="colorTag" name="colorText" />
              </span>
            </label>
            <label>
              Season
              <select [(ngModel)]="season" name="season">
                <option [ngValue]="null">— none —</option>
                <option value="SUMMER">Summer</option>
                <option value="WINTER">Winter</option>
                <option value="ALL">All year</option>
              </select>
            </label>
            <div class="actions">
              <button type="button" (click)="reset()">Retake</button>
              <button class="primary" type="submit" [disabled]="saving()">
                {{ saving() ? 'Saving…' : 'Save to wardrobe' }}
              </button>
            </div>
          </form>
        </div>
      }

      <canvas #canvas hidden></canvas>
    </section>
  `,
  styles: [
    `
      .scan {
        max-width: 640px;
        margin: 0 auto;
      }
      video {
        width: 100%;
        border-radius: 12px;
        background: #111;
        aspect-ratio: 3 / 4;
        object-fit: cover;
      }
      video.hidden {
        display: none;
      }
      .actions {
        display: flex;
        gap: 0.75rem;
        margin-top: 0.75rem;
        align-items: center;
      }
      button {
        border: 1px solid #ccc;
        background: #fff;
        border-radius: 8px;
        padding: 0.5rem 1rem;
        cursor: pointer;
      }
      button.primary {
        background: #6a4cff;
        color: #fff;
        border-color: #6a4cff;
      }
      .file {
        color: #6a4cff;
        cursor: pointer;
        font-weight: 600;
      }
      .processing {
        text-align: center;
        padding: 3rem 0;
        color: #666;
      }
      .spinner {
        width: 42px;
        height: 42px;
        border: 4px solid #eee;
        border-top-color: #6a4cff;
        border-radius: 50%;
        margin: 0 auto 1rem;
        animation: spin 0.9s linear infinite;
      }
      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }
      .confirm {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1.5rem;
      }
      @media (max-width: 620px) {
        .confirm {
          grid-template-columns: 1fr;
        }
      }
      .preview {
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 220px;
      }
      .preview img {
        max-width: 100%;
        max-height: 300px;
        object-fit: contain;
      }
      form label {
        display: block;
        margin-bottom: 0.9rem;
        font-size: 0.85rem;
        color: #555;
      }
      form select,
      form input[type='text'] {
        width: 100%;
        padding: 0.45rem;
        border: 1px solid #ddd;
        border-radius: 8px;
        margin-top: 0.25rem;
      }
      .color-row {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }
      .err {
        color: #c0392b;
      }
    `,
  ],
})
export class ScanFlowComponent implements OnDestroy {
  private readonly garmentApi = inject(GarmentService);
  private readonly store = inject(WardrobeStore);
  private readonly router = inject(Router);

  private readonly videoRef = viewChild<ElementRef<HTMLVideoElement>>('video');
  private readonly canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('canvas');

  protected readonly allCategories = WEARABLE_CATEGORIES;
  protected readonly phase = signal<Phase>('capture');
  protected readonly streaming = signal(false);
  protected readonly cameraError = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly result = signal<ScanResult | null>(null);

  protected category: GarmentCategory = 'TOP';
  protected colorTag = '#888888';
  protected season: Season | null = null;

  private stream: MediaStream | null = null;

  async startCamera(): Promise<void> {
    this.cameraError.set(null);
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false,
      });
      const video = this.videoRef()?.nativeElement;
      if (video) {
        video.srcObject = this.stream;
        this.streaming.set(true);
      }
    } catch {
      this.cameraError.set('Camera unavailable. You can upload a photo instead.');
    }
  }

  async capture(): Promise<void> {
    const video = this.videoRef()?.nativeElement;
    const canvas = this.canvasRef()?.nativeElement;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth || 720;
    canvas.height = video.videoHeight || 960;
    canvas.getContext('2d')?.drawImage(video, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob((b) => resolve(b), 'image/jpeg', 0.9),
    );
    if (blob) await this.upload(blob, 'capture.jpg');
  }

  async onFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) await this.upload(file, file.name);
  }

  private async upload(blob: Blob, filename: string): Promise<void> {
    this.stopCamera();
    this.phase.set('processing');
    try {
      const result = await firstValueFrom(this.garmentApi.scan(blob, filename));
      this.result.set(result);
      this.category = result.suggestedCategory;
      this.colorTag = normaliseHex(result.suggestedColorTag) ?? '#888888';
      this.season = null;
      this.phase.set('confirm');
    } catch {
      this.cameraError.set('Scan failed. Is the backend running?');
      this.phase.set('capture');
    }
  }

  async confirm(): Promise<void> {
    const result = this.result();
    if (!result) return;
    this.saving.set(true);
    const created = await this.store.add({
      category: this.category,
      imageUrl: result.imageUrl,
      colorTag: this.colorTag,
      pattern: result.suggestedPattern,
      season: this.season,
    });
    this.saving.set(false);
    if (created) {
      await this.router.navigate(['/wardrobe']);
    }
  }

  reset(): void {
    this.result.set(null);
    this.phase.set('capture');
  }

  private stopCamera(): void {
    this.stream?.getTracks().forEach((t) => t.stop());
    this.stream = null;
    this.streaming.set(false);
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }
}

function normaliseHex(value: string | null): string | null {
  if (!value) return null;
  return /^#[0-9a-fA-F]{6}$/.test(value) ? value : null;
}
