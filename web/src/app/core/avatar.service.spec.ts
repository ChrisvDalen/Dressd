import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AvatarService } from './avatar.service';
import { Avatar, BodyTypePreset, SaveAvatarRequest } from './models';

function avatar(id: string, overrides: Partial<Avatar> = {}): Avatar {
  return {
    id,
    ownerId: 'owner-1',
    name: 'Me',
    bodyType: 'AVERAGE',
    proportions: { height: 1, shoulderWidth: 1, hipWidth: 1 },
    ...overrides,
  };
}

const preset: BodyTypePreset = {
  bodyType: 'CURVY',
  label: 'Curvy',
  proportions: { height: 0.98, shoulderWidth: 1.05, hipWidth: 1.15 },
};

describe('AvatarService', () => {
  let service: AvatarService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AvatarService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the body-type presets from the dedicated endpoint', () => {
    let received: BodyTypePreset[] | undefined;
    service.bodyTypes().subscribe((b) => (received = b));

    const req = http.expectOne('/api/avatars/body-types');
    expect(req.request.method).toBe('GET');
    req.flush([preset]);

    expect(received).toHaveLength(1);
    expect(received?.[0].label).toBe('Curvy');
  });

  it('lists avatars', () => {
    let received: Avatar[] | undefined;
    service.list().subscribe((a) => (received = a));

    const req = http.expectOne('/api/avatars');
    req.flush([avatar('a1')]);

    expect(received?.[0].id).toBe('a1');
  });

  it('creates an avatar with the full request body', () => {
    const request: SaveAvatarRequest = { name: 'New', bodyType: 'SLIM' };
    let received: Avatar | undefined;
    service.create(request).subscribe((a) => (received = a));

    const req = http.expectOne('/api/avatars');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(avatar('a2'));

    expect(received?.id).toBe('a2');
  });

  it('updates the addressed avatar by id', () => {
    const request: SaveAvatarRequest = { name: 'Renamed', bodyType: 'CURVY' };
    let received: Avatar | undefined;
    service.update('a1', request).subscribe((a) => (received = a));

    const req = http.expectOne('/api/avatars/a1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(avatar('a1', { name: 'Renamed', bodyType: 'CURVY' }));

    expect(received?.name).toBe('Renamed');
  });
});
