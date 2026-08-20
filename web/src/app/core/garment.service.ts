import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateGarmentRequest,
  Garment,
  GarmentCategory,
  Page,
  ScanResult,
  Season,
  UpdateGarmentRequest,
} from './models';

export interface WardrobeFilter {
  category?: GarmentCategory | null;
  color?: string | null;
  season?: Season | null;
}

export interface PageRequest {
  page?: number;
  size?: number;
}

/** Client for the wardrobe-service (scan + garment CRUD). */
@Injectable({ providedIn: 'root' })
export class GarmentService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/garments';

  /** Flow A step 2-4: upload a photo, get a cut-out preview + suggestions. */
  scan(photo: Blob, filename = 'scan.png'): Observable<ScanResult> {
    const form = new FormData();
    form.append('file', photo, filename);
    return this.http.post<ScanResult>('/api/scan', form);
  }

  list(filter: WardrobeFilter = {}, page: PageRequest = {}): Observable<Page<Garment>> {
    let params = new HttpParams();
    if (filter.category) params = params.set('category', filter.category);
    if (filter.color) params = params.set('color', filter.color);
    if (filter.season) params = params.set('season', filter.season);
    if (page.page !== undefined) params = params.set('page', page.page);
    if (page.size !== undefined) params = params.set('size', page.size);
    return this.http.get<Page<Garment>>(this.base, { params });
  }

  create(request: CreateGarmentRequest): Observable<Garment> {
    return this.http.post<Garment>(this.base, request);
  }

  update(id: string, request: UpdateGarmentRequest): Observable<Garment> {
    return this.http.patch<Garment>(`${this.base}/${id}`, request);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
