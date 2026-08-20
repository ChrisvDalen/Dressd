import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Outfit, Page, SaveOutfitRequest } from './models';
import { PageRequest } from './garment.service';

/** Client for the outfit-composer-service. */
@Injectable({ providedIn: 'root' })
export class OutfitService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/outfits';

  list(page: PageRequest = {}): Observable<Page<Outfit>> {
    let params = new HttpParams();
    if (page.page !== undefined) params = params.set('page', page.page);
    if (page.size !== undefined) params = params.set('size', page.size);
    return this.http.get<Page<Outfit>>(this.base, { params });
  }

  get(id: string): Observable<Outfit> {
    return this.http.get<Outfit>(`${this.base}/${id}`);
  }

  create(request: SaveOutfitRequest): Observable<Outfit> {
    return this.http.post<Outfit>(this.base, request);
  }

  update(id: string, request: SaveOutfitRequest): Observable<Outfit> {
    return this.http.put<Outfit>(`${this.base}/${id}`, request);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
