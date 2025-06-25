# AR Filter Models

This directory contains 3D models for AR filters used in the SnapConnect app.

## Required Models

The application expects the following model files in GLB format:

1. `sunglasses.glb` - 3D model of sunglasses for face filter
2. `party_hat.glb` - 3D model of a party hat for head filter
3. `bunny_ears.glb` - 3D model of bunny ears for head filter
4. `face_mask.glb` - 3D model of a face mask filter

## How to Add Models

1. Create or download 3D models in GLB format
2. Ensure models are properly rigged for face tracking
3. Place the GLB files in this directory
4. The models should be named exactly as listed above

## Model Requirements

- Format: GLB (binary glTF)
- Optimized for mobile devices (low poly count)
- Properly rigged for face tracking
- Correctly scaled and positioned for face alignment
- Textures should be embedded in the GLB file

## Resources for Free AR Models

- [Sketchfab](https://sketchfab.com/) - Many free and paid 3D models
- [Google Poly](https://poly.google.com/) - Archive of free 3D models
- [TurboSquid](https://www.turbosquid.com/) - 3D models marketplace with free options

## Testing AR Models

Before adding models to the app, test them with AR tools like:
- [Google's Model Viewer](https://modelviewer.dev/)
- [Sceneform](https://github.com/SceneView/sceneform-android)
- [ARCore Scene Viewer](https://developers.google.com/ar/develop/scene-viewer)

## Notes

The current implementation uses placeholder models. For production, replace these with properly designed and optimized 3D models for the best AR experience. 