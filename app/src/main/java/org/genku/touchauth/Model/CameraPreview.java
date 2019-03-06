package org.genku.touchauth.Model;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.Gravity;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.io.IOException;

public class CameraPreview extends TextureView implements TextureView.SurfaceTextureListener {
    private static Context context;
    private Camera mCamera;
    public CameraPreview(Context context,Camera camera) {
        super(context);
        mCamera = camera;
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        setLayoutParams(new FrameLayout.LayoutParams(
                previewSize.width, previewSize.height, Gravity.CENTER));

        try{
            mCamera.setPreviewTexture(surface);
        } catch (IOException t) {}

        mCamera.startPreview();
        this.setVisibility(INVISIBLE); // Make the surface invisible as soon as it is created
}

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
