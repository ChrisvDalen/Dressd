import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Avatar, BodyTypePreset, SaveAvatarRequest } from './models';

/** Client for the avatar-service. */
@Injectable({ providedIn: 'root' })
export class AvatarService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/avatars';

  bodyTypes(): Observable<BodyTypePreset[]> {
    return this.http.get<BodyTypePreset[]>(`${this.base}/body-types`);
  }

  list(): Observable<Avatar[]> {
    return this.http.get<Avatar[]>(this.base);
  }

  create(request: SaveAvatarRequest): Observable<Avatar> {
    return this.http.post<Avatar>(this.base, request);
  }

  update(id: string, request: SaveAvatarRequest): Observable<Avatar> {
    return this.http.put<Avatar>(`${this.base}/${id}`, request);
  }
}
