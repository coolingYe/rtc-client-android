/*
 *  Copyright 2012 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#include "pc/test/integration_test_helpers.h"

namespace webrtc {

PeerConnectionInterface::RTCOfferAnswerOptions IceRestartOfferAnswerOptions() {
  PeerConnectionInterface::RTCOfferAnswerOptions options;
  options.ice_restart = true;
  return options;
}

void RemoveSsrcsAndMsids(cricket::SessionDescription* desc) {
  for (ContentInfo& content : desc->contents()) {
    content.media_description()->mutable_streams().clear();
  }
  desc->set_msid_supported(false);
  desc->set_msid_signaling(0);
}

void RemoveSsrcsAndKeepMsids(cricket::SessionDescription* desc) {
  for (ContentInfo& content : desc->contents()) {
    std::string track_id;
    std::vector<std::string> stream_ids;
    if (!content.media_description()->streams().empty()) {
      const StreamParams& first_stream =
          content.media_description()->streams()[0];
      track_id = first_stream.id;
      stream_ids = first_stream.stream_ids();
    }
    content.media_description()->mutable_streams().clear();
    StreamParams new_stream;
    new_stream.id = track_id;
    new_stream.set_stream_ids(stream_ids);
    content.media_description()->AddStream(new_stream);
  }
}

int FindFirstMediaStatsIndexByKind(
    const std::string& kind,
    const std::vector<const webrtc::RTCMediaStreamTrackStats*>&
        media_stats_vec) {
  for (size_t i = 0; i < media_stats_vec.size(); i++) {
    if (media_stats_vec[i]->kind.ValueToString() == kind) {
      return i;
    }
  }
  return -1;
}

}  // namespace webrtc
