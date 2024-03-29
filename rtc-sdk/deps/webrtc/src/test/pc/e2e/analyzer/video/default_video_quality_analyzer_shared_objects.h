/*
 *  Copyright (c) 2021 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#ifndef TEST_PC_E2E_ANALYZER_VIDEO_DEFAULT_VIDEO_QUALITY_ANALYZER_SHARED_OBJECTS_H_
#define TEST_PC_E2E_ANALYZER_VIDEO_DEFAULT_VIDEO_QUALITY_ANALYZER_SHARED_OBJECTS_H_

#include <memory>
#include <string>
#include <utility>
#include <vector>

#include "absl/types/optional.h"
#include "api/numerics/samples_stats_counter.h"
#include "api/units/timestamp.h"

namespace webrtc {
namespace webrtc_pc_e2e {

// WebRTC will request a key frame after 3 seconds if no frames were received.
// We assume max frame rate ~60 fps, so 270 frames will cover max freeze without
// key frame request.
constexpr size_t kDefaultMaxFramesInFlightPerStream = 270;

class RateCounter {
 public:
  void AddEvent(Timestamp event_time);

  bool IsEmpty() const { return event_first_time_ == event_last_time_; }

  double GetEventsPerSecond() const;

 private:
  Timestamp event_first_time_ = Timestamp::MinusInfinity();
  Timestamp event_last_time_ = Timestamp::MinusInfinity();
  int64_t event_count_ = 0;
};

struct FrameCounters {
  // Count of frames, that were passed into WebRTC pipeline by video stream
  // source.
  int64_t captured = 0;
  // Count of frames that reached video encoder.
  int64_t pre_encoded = 0;
  // Count of encoded images that were produced by encoder for all requested
  // spatial layers and simulcast streams.
  int64_t encoded = 0;
  // Count of encoded images received in decoder for all requested spatial
  // layers and simulcast streams.
  int64_t received = 0;
  // Count of frames that were produced by decoder.
  int64_t decoded = 0;
  // Count of frames that went out from WebRTC pipeline to video sink.
  int64_t rendered = 0;
  // Count of frames that were dropped in any point between capturing and
  // rendering.
  int64_t dropped = 0;
};

// Contains information about the codec that was used for encoding or decoding
// the stream.
struct StreamCodecInfo {
  // Codec implementation name.
  std::string codec_name;
  // Id of the first frame for which this codec was used.
  uint16_t first_frame_id;
  // Id of the last frame for which this codec was used.
  uint16_t last_frame_id;
  // Timestamp when the first frame was handled by the encode/decoder.
  Timestamp switched_on_at = Timestamp::PlusInfinity();
  // Timestamp when this codec was used last time.
  Timestamp switched_from_at = Timestamp::PlusInfinity();
};

struct StreamStats {
  explicit StreamStats(Timestamp stream_started_time)
      : stream_started_time(stream_started_time) {}

  // The time when the first frame of this stream was captured.
  Timestamp stream_started_time;

  SamplesStatsCounter psnr;
  SamplesStatsCounter ssim;
  // Time from frame encoded (time point on exit from encoder) to the
  // encoded image received in decoder (time point on entrance to decoder).
  SamplesStatsCounter transport_time_ms;
  // Time from frame was captured on device to time frame was displayed on
  // device.
  SamplesStatsCounter total_delay_incl_transport_ms;
  // Time between frames out from renderer.
  SamplesStatsCounter time_between_rendered_frames_ms;
  RateCounter encode_frame_rate;
  SamplesStatsCounter encode_time_ms;
  SamplesStatsCounter decode_time_ms;
  // Time from last packet of frame is received until it's sent to the renderer.
  SamplesStatsCounter receive_to_render_time_ms;
  // Max frames skipped between two nearest.
  SamplesStatsCounter skipped_between_rendered;
  // In the next 2 metrics freeze is a pause that is longer, than maximum:
  //  1. 150ms
  //  2. 3 * average time between two sequential frames.
  // Item 1 will cover high fps video and is a duration, that is noticeable by
  // human eye. Item 2 will cover low fps video like screen sharing.
  // Freeze duration.
  SamplesStatsCounter freeze_time_ms;
  // Mean time between one freeze end and next freeze start.
  SamplesStatsCounter time_between_freezes_ms;
  SamplesStatsCounter resolution_of_rendered_frame;
  SamplesStatsCounter target_encode_bitrate;

  int64_t total_encoded_images_payload = 0;
  int64_t dropped_by_encoder = 0;
  int64_t dropped_before_encoder = 0;

  // Vector of encoders used for this stream by sending client.
  std::vector<StreamCodecInfo> encoders;
  // Vectors of decoders used for this stream by receiving client.
  std::vector<StreamCodecInfo> decoders;
};

struct AnalyzerStats {
  // Size of analyzer internal comparisons queue, measured when new element
  // id added to the queue.
  SamplesStatsCounter comparisons_queue_size;
  // Number of performed comparisons of 2 video frames from captured and
  // rendered streams.
  int64_t comparisons_done = 0;
  // Number of cpu overloaded comparisons. Comparison is cpu overloaded if it is
  // queued when there are too many not processed comparisons in the queue.
  // Overloaded comparison doesn't include metrics like SSIM and PSNR that
  // require heavy computations.
  int64_t cpu_overloaded_comparisons_done = 0;
  // Number of memory overloaded comparisons. Comparison is memory overloaded if
  // it is queued when its captured frame was already removed due to high memory
  // usage for that video stream.
  int64_t memory_overloaded_comparisons_done = 0;
  // Count of frames in flight in analyzer measured when new comparison is added
  // and after analyzer was stopped.
  SamplesStatsCounter frames_in_flight_left_count;
};

struct StatsKey {
  StatsKey(std::string stream_label, std::string sender, std::string receiver)
      : stream_label(std::move(stream_label)),
        sender(std::move(sender)),
        receiver(std::move(receiver)) {}

  std::string ToString() const;

  // Label of video stream to which stats belongs to.
  std::string stream_label;
  // Name of the peer which send this stream.
  std::string sender;
  // Name of the peer on which stream was received.
  std::string receiver;
};

// Required to use StatsKey as std::map key.
bool operator<(const StatsKey& a, const StatsKey& b);
bool operator==(const StatsKey& a, const StatsKey& b);

}  // namespace webrtc_pc_e2e
}  // namespace webrtc

#endif  // TEST_PC_E2E_ANALYZER_VIDEO_DEFAULT_VIDEO_QUALITY_ANALYZER_SHARED_OBJECTS_H_
