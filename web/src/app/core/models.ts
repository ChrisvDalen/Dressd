// Domain types mirrored from the backend (SPEC.md section 4).

export type GarmentCategory =
  | 'TOP'
  | 'BOTTOM'
  | 'SOCKS'
  | 'LAYER'
  | 'SHOES'
  | 'ACCESSORY';

export const WEARABLE_CATEGORIES: GarmentCategory[] = [
  'TOP',
  'BOTTOM',
  'SOCKS',
  'LAYER',
  'SHOES',
  'ACCESSORY',
];

/** Categories that get their own swipe row in the outfit builder (flow B). */
export const SWIPE_CATEGORIES: GarmentCategory[] = ['LAYER', 'TOP', 'BOTTOM', 'SHOES', 'SOCKS'];

export type Season = 'SUMMER' | 'WINTER' | 'ALL';

/** Envelope every paginated list endpoint returns (com.dressd.common.web.PageResponse). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type BodyType = 'SLIM' | 'AVERAGE' | 'CURVY' | 'CUSTOM';

export interface AnchorPoints {
  shoulderY: number;
  waistY: number;
  hemY: number;
  widthScale: number;
}

export interface Garment {
  id: string;
  ownerId: string;
  category: GarmentCategory;
  imageUrl: string;
  colorTag: string | null;
  pattern: string | null;
  season: Season | null;
  anchorPoints: AnchorPoints;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGarmentRequest {
  category: GarmentCategory;
  imageUrl: string;
  colorTag?: string | null;
  pattern?: string | null;
  season?: Season | null;
  anchorPoints?: AnchorPoints | null;
}

export interface UpdateGarmentRequest {
  category?: GarmentCategory;
  colorTag?: string | null;
  pattern?: string | null;
  season?: Season | null;
  anchorPoints?: AnchorPoints | null;
}

export interface ScanResult {
  imageUrl: string;
  suggestedCategory: GarmentCategory;
  suggestedColorTag: string | null;
  suggestedPattern: string | null;
}

export interface Proportions {
  height: number;
  shoulderWidth: number;
  hipWidth: number;
}

export interface Avatar {
  id: string;
  ownerId: string;
  name: string | null;
  bodyType: BodyType;
  proportions: Proportions;
}

export interface BodyTypePreset {
  bodyType: BodyType;
  label: string;
  proportions: Proportions;
}

export interface SaveAvatarRequest {
  name?: string | null;
  bodyType: BodyType;
  proportions?: Proportions | null;
}

export interface GarmentLayer {
  garmentId: string;
  category: GarmentCategory;
  zIndex: number;
  offsetX: number;
  offsetY: number;
  scale: number;
}

export interface Outfit {
  id: string;
  ownerId: string;
  avatarId: string;
  name: string | null;
  garmentLayers: GarmentLayer[];
  createdAt: string;
  updatedAt: string;
}

export interface SaveOutfitRequest {
  avatarId: string;
  name?: string | null;
  garmentLayers: GarmentLayer[];
}
