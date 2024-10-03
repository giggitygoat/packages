// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import android.content.res.AssetManager;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import io.flutter.plugins.googlemaps.Messages.MapsCallbackApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.annotation.NonNull;

class GroundOverlaysController {
  private GoogleMap googleMap;
  public final Map<String, GroundOverlayController> groundOverlayIdToController;
  private final Map<String, String> googleMapsGroundOverlayIdToDartOverlayId;
  private final @NonNull MapsCallbackApi flutterApi;
  private final AssetManager assetManager;
  private final float density;

  GroundOverlaysController(@NonNull MapsCallbackApi flutterApi, AssetManager assetManager, float density) {
    this.flutterApi = flutterApi;
    this.groundOverlayIdToController = new HashMap<>();
    this.googleMapsGroundOverlayIdToDartOverlayId = new HashMap<>();
    this.assetManager = assetManager;
    this.density = density;
  }



  void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
  }

  void addGroundOverlays(@NonNull List<Messages.PlatformGroundOverlay> groundOverlaysToAdd) {
    if (groundOverlaysToAdd != null) {
      for (Messages.PlatformGroundOverlay groundOverlayToAdd : groundOverlaysToAdd) {
        addGroundOverlay(groundOverlayToAdd);
      }
    }
  }

  private void addGroundOverlay(Messages.PlatformGroundOverlay groundOverlay) {
    if (groundOverlay == null) {
      return;
    }

    GroundOverlayBuilder groundOverlayBuilder = new GroundOverlayBuilder();
    String groundOverlayId = Convert.interpretGroundOverlayOptions(groundOverlay, groundOverlayBuilder, assetManager, density);
    GroundOverlayOptions options = groundOverlayBuilder.build();
    addGroundOverlay(groundOverlayId, options, groundOverlayBuilder.consumeTapEvents());
  }

  private void addGroundOverlay(
    String groundOverlayId, GroundOverlayOptions groundOverlayOptions, boolean consumeTapEvents) {
    final GroundOverlay groundOverlay = googleMap.addGroundOverlay(groundOverlayOptions);

    GroundOverlayController controller = new GroundOverlayController(groundOverlay, consumeTapEvents);
    groundOverlayIdToController.put(groundOverlayId, controller);
    googleMapsGroundOverlayIdToDartOverlayId.put(groundOverlay.getId(), groundOverlayId);
  }

  boolean onGroundOverlayTap(String googleOverlayId) {
    String overlayId = googleMapsGroundOverlayIdToDartOverlayId.get(googleOverlayId);
    if (overlayId == null) {
      return false;
    }
    flutterApi.onGroundOverlayTap(overlayId, new NoOpVoidResult());
    GroundOverlayController groundOverlayController = groundOverlayIdToController.get(overlayId);
    if (groundOverlayController != null) {
      return groundOverlayController.consumeTapEvents();
    }
    return false;
  }

  void changeGroundOverlays(@NonNull List<Messages.PlatformGroundOverlay> groundOverlaysToChange) {
    if (groundOverlaysToChange != null) {
      for (Messages.PlatformGroundOverlay groundOverlayToChange : groundOverlaysToChange) {
        changeGroundOverlay(groundOverlayToChange);
      }
    }
  }

  private void changeGroundOverlay(Messages.PlatformGroundOverlay groundOverlay) {
    if (groundOverlay == null) {
      return;
    }
    String groundOverlayId = getGroundOverlayId(groundOverlay);
    GroundOverlayController groundOverlayController = groundOverlayIdToController.get(groundOverlayId);
    if (groundOverlayController != null) {
      Convert.interpretGroundOverlayOptions(groundOverlay, groundOverlayController, assetManager, density);
    }
  }

  void removeGroundOverlays(@NonNull List<String> groundOverlaysToRemove) {
    if (groundOverlaysToRemove == null) {
      return;
    }

    for (String rawGroundOverlayId : groundOverlaysToRemove) {
      if (rawGroundOverlayId == null) {
        continue;
      }
      String groundOverlayId = (String) rawGroundOverlayId;
      final GroundOverlayController groundOverlayController = groundOverlayIdToController.remove(groundOverlayId);
      if (groundOverlayController != null) {
        groundOverlayController.remove();
        googleMapsGroundOverlayIdToDartOverlayId.remove(groundOverlayController.getGoogleMapsGroundOverlayId());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static String getGroundOverlayId(Object groundOverlay) {
    Messages.PlatformGroundOverlay groundOverlayMap = (Messages.PlatformGroundOverlay) groundOverlay;
    return groundOverlayMap.getGroundOverlayId();
  }
}