import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'wardrobe' },
  {
    path: 'wardrobe',
    title: 'Wardrobe · Dressd',
    loadComponent: () =>
      import('./features/wardrobe/wardrobe-grid.component').then((m) => m.WardrobeGridComponent),
  },
  {
    path: 'scan',
    title: 'Scan · Dressd',
    loadComponent: () =>
      import('./features/scan/scan-flow.component').then((m) => m.ScanFlowComponent),
  },
  {
    path: 'builder',
    title: 'Outfit builder · Dressd',
    loadComponent: () =>
      import('./features/builder/outfit-builder.component').then((m) => m.OutfitBuilderComponent),
  },
  {
    path: 'outfits',
    title: 'Outfits · Dressd',
    loadComponent: () =>
      import('./features/outfits/outfits.component').then((m) => m.OutfitsComponent),
  },
  { path: '**', redirectTo: 'wardrobe' },
];
