/*
 *  Copyright (c) 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#ifndef MODULES_VIDEO_CODING_DECODER_DATABASE_H_
#define MODULES_VIDEO_CODING_DECODER_DATABASE_H_

#include <stdint.h>

#include <map>

#include "absl/types/optional.h"
#include "api/video_codecs/video_decoder.h"
#include "modules/video_coding/encoded_frame.h"
#include "modules/video_coding/generic_decoder.h"

namespace webrtc {

class VCMDecoderDataBase {
 public:
  VCMDecoderDataBase() = default;
  VCMDecoderDataBase(const VCMDecoderDataBase&) = delete;
  VCMDecoderDataBase& operator=(const VCMDecoderDataBase&) = delete;
  ~VCMDecoderDataBase() = default;

  bool DeregisterExternalDecoder(uint8_t payload_type);
  void RegisterExternalDecoder(uint8_t payload_type,
                               VideoDecoder* external_decoder);
  bool IsExternalDecoderRegistered(uint8_t payload_type) const;

  bool RegisterReceiveCodec(uint8_t payload_type,
                            const VideoCodec& receive_codec,
                            int number_of_cores);
  bool DeregisterReceiveCodec(uint8_t payload_type);

  // Returns a decoder specified by frame.PayloadType. The decoded frame
  // callback of the decoder is set to `decoded_frame_callback`. If no such
  // decoder already exists an instance will be created and initialized.
  // nullptr is returned if no decoder with the specified payload type was found
  // and the function failed to create one.
  VCMGenericDecoder* GetDecoder(
      const VCMEncodedFrame& frame,
      VCMDecodedFrameCallback* decoded_frame_callback);

 private:
  struct DecoderSettings {
    VideoCodec settings;
    int number_of_cores;
  };

  void CreateAndInitDecoder(const VCMEncodedFrame& frame);

  absl::optional<uint8_t> current_payload_type_;
  absl::optional<VCMGenericDecoder> current_decoder_;
  // Initialization paramaters for decoders keyed by payload type.
  std::map<uint8_t, DecoderSettings> decoder_settings_;
  // Decoders keyed by payload type.
  std::map<uint8_t, VideoDecoder*> decoders_;
};

}  // namespace webrtc

#endif  // MODULES_VIDEO_CODING_DECODER_DATABASE_H_
