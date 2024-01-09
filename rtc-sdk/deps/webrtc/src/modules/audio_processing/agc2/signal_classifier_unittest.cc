/*
 *  Copyright (c) 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#include "modules/audio_processing/agc2/signal_classifier.h"

#include <array>
#include <functional>
#include <limits>

#include "api/function_view.h"
#include "modules/audio_processing/agc2/agc2_testing_common.h"
#include "modules/audio_processing/logging/apm_data_dumper.h"
#include "rtc_base/gunit.h"
#include "rtc_base/random.h"

namespace webrtc {
namespace {
constexpr int kNumIterations = 100;

// Runs the signal classifier on audio generated by 'sample_generator'
// for kNumIterations. Returns the number of frames classified as noise.
float RunClassifier(rtc::FunctionView<float()> sample_generator,
                    int sample_rate_hz) {
  ApmDataDumper data_dumper(0);
  SignalClassifier classifier(&data_dumper);
  std::array<float, 480> signal;
  classifier.Initialize(sample_rate_hz);
  const size_t samples_per_channel = rtc::CheckedDivExact(sample_rate_hz, 100);
  int number_of_noise_frames = 0;
  for (int i = 0; i < kNumIterations; ++i) {
    for (size_t j = 0; j < samples_per_channel; ++j) {
      signal[j] = sample_generator();
    }
    number_of_noise_frames +=
        classifier.Analyze({&signal[0], samples_per_channel}) ==
        SignalClassifier::SignalType::kStationary;
  }
  return number_of_noise_frames;
}

class SignalClassifierParametrization : public ::testing::TestWithParam<int> {
 protected:
  int sample_rate_hz() const { return GetParam(); }
};

// White random noise is stationary, but does not trigger the detector
// every frame due to the randomness.
TEST_P(SignalClassifierParametrization, WhiteNoise) {
  test::WhiteNoiseGenerator gen(/*min_amplitude=*/test::kMinS16,
                                /*max_amplitude=*/test::kMaxS16);
  const int number_of_noise_frames = RunClassifier(gen, sample_rate_hz());
  EXPECT_GT(number_of_noise_frames, kNumIterations / 2);
}

// Sine curves are (very) stationary. They trigger the detector all
// the time. Except for a few initial frames.
TEST_P(SignalClassifierParametrization, SineTone) {
  test::SineGenerator gen(/*amplitude=*/test::kMaxS16, /*frequency_hz=*/600.0f,
                          sample_rate_hz());
  const int number_of_noise_frames = RunClassifier(gen, sample_rate_hz());
  EXPECT_GE(number_of_noise_frames, kNumIterations - 5);
}

// Pulses are transient if they are far enough apart. They shouldn't
// trigger the noise detector.
TEST_P(SignalClassifierParametrization, PulseTone) {
  test::PulseGenerator gen(/*pulse_amplitude=*/test::kMaxS16,
                           /*no_pulse_amplitude=*/10.0f, /*frequency_hz=*/20.0f,
                           sample_rate_hz());
  const int number_of_noise_frames = RunClassifier(gen, sample_rate_hz());
  EXPECT_EQ(number_of_noise_frames, 0);
}

INSTANTIATE_TEST_SUITE_P(GainController2SignalClassifier,
                         SignalClassifierParametrization,
                         ::testing::Values(8000, 16000, 32000, 48000));

}  // namespace
}  // namespace webrtc
