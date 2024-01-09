/*
 *  Copyright (c) 2019 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#ifndef MODULES_RTP_RTCP_SOURCE_RTP_SENDER_EGRESS_H_
#define MODULES_RTP_RTCP_SOURCE_RTP_SENDER_EGRESS_H_

#include <map>
#include <memory>
#include <utility>
#include <vector>

#include "absl/types/optional.h"
#include "api/call/transport.h"
#include "api/rtc_event_log/rtc_event_log.h"
#include "api/sequence_checker.h"
#include "api/task_queue/task_queue_base.h"
#include "api/units/data_rate.h"
#include "modules/remote_bitrate_estimator/test/bwe_test_logging.h"
#include "modules/rtp_rtcp/include/rtp_rtcp_defines.h"
#include "modules/rtp_rtcp/source/rtp_packet_history.h"
#include "modules/rtp_rtcp/source/rtp_packet_to_send.h"
#include "modules/rtp_rtcp/source/rtp_rtcp_interface.h"
#include "modules/rtp_rtcp/source/rtp_sequence_number_map.h"
#include "rtc_base/rate_statistics.h"
#include "rtc_base/synchronization/mutex.h"
#include "rtc_base/system/no_unique_address.h"
#include "rtc_base/task_utils/pending_task_safety_flag.h"
#include "rtc_base/task_utils/repeating_task.h"
#include "rtc_base/thread_annotations.h"

namespace webrtc {

class RtpSenderEgress {
 public:
  // Helper class that redirects packets directly to the send part of this class
  // without passing through an actual paced sender.
  class NonPacedPacketSender : public RtpPacketSender {
   public:
    NonPacedPacketSender(RtpSenderEgress* sender,
                         SequenceNumberAssigner* sequence_number_assigner,
                         bool deferred_sequencing);
    virtual ~NonPacedPacketSender();

    void EnqueuePackets(
        std::vector<std::unique_ptr<RtpPacketToSend>> packets) override;

   private:
    void PrepareForSend(RtpPacketToSend* packet);
    const bool deferred_sequencing_;
    uint16_t transport_sequence_number_;
    RtpSenderEgress* const sender_;
    SequenceNumberAssigner* sequence_number_assigner_;
  };

  RtpSenderEgress(const RtpRtcpInterface::Configuration& config,
                  RtpPacketHistory* packet_history);
  ~RtpSenderEgress();

  void SendPacket(RtpPacketToSend* packet, const PacedPacketInfo& pacing_info)
      RTC_LOCKS_EXCLUDED(lock_);
  uint32_t Ssrc() const { return ssrc_; }
  absl::optional<uint32_t> RtxSsrc() const { return rtx_ssrc_; }
  absl::optional<uint32_t> FlexFecSsrc() const { return flexfec_ssrc_; }

  RtpSendRates GetSendRates() const RTC_LOCKS_EXCLUDED(lock_);
  void GetDataCounters(StreamDataCounters* rtp_stats,
                       StreamDataCounters* rtx_stats) const
      RTC_LOCKS_EXCLUDED(lock_);

  void ForceIncludeSendPacketsInAllocation(bool part_of_allocation)
      RTC_LOCKS_EXCLUDED(lock_);
  bool MediaHasBeenSent() const RTC_LOCKS_EXCLUDED(lock_);
  void SetMediaHasBeenSent(bool media_sent) RTC_LOCKS_EXCLUDED(lock_);
  void SetTimestampOffset(uint32_t timestamp) RTC_LOCKS_EXCLUDED(lock_);

  // For each sequence number in `sequence_number`, recall the last RTP packet
  // which bore it - its timestamp and whether it was the first and/or last
  // packet in that frame. If all of the given sequence numbers could be
  // recalled, return a vector with all of them (in corresponding order).
  // If any could not be recalled, return an empty vector.
  std::vector<RtpSequenceNumberMap::Info> GetSentRtpPacketInfos(
      rtc::ArrayView<const uint16_t> sequence_numbers) const
      RTC_LOCKS_EXCLUDED(lock_);

  void SetFecProtectionParameters(const FecProtectionParams& delta_params,
                                  const FecProtectionParams& key_params);
  std::vector<std::unique_ptr<RtpPacketToSend>> FetchFecPackets();

 private:
  // Maps capture time in milliseconds to send-side delay in milliseconds.
  // Send-side delay is the difference between transmission time and capture
  // time.
  typedef std::map<int64_t, int> SendDelayMap;

  RtpSendRates GetSendRatesLocked(int64_t now_ms) const
      RTC_EXCLUSIVE_LOCKS_REQUIRED(lock_);
  bool HasCorrectSsrc(const RtpPacketToSend& packet) const;
  void AddPacketToTransportFeedback(uint16_t packet_id,
                                    const RtpPacketToSend& packet,
                                    const PacedPacketInfo& pacing_info);
  void UpdateDelayStatistics(int64_t capture_time_ms,
                             int64_t now_ms,
                             uint32_t ssrc);
  void RecomputeMaxSendDelay() RTC_EXCLUSIVE_LOCKS_REQUIRED(lock_);
  void UpdateOnSendPacket(int packet_id,
                          int64_t capture_time_ms,
                          uint32_t ssrc);
  // Sends packet on to `transport_`, leaving the RTP module.
  bool SendPacketToNetwork(const RtpPacketToSend& packet,
                           const PacketOptions& options,
                           const PacedPacketInfo& pacing_info);

  void UpdateRtpStats(int64_t now_ms,
                      uint32_t packet_ssrc,
                      RtpPacketMediaType packet_type,
                      RtpPacketCounter counter,
                      size_t packet_size);
#if BWE_TEST_LOGGING_COMPILE_TIME_ENABLE
  void BweTestLoggingPlot(int64_t now_ms, uint32_t packet_ssrc);
#endif

  // Called on a timer, once a second, on the worker_queue_.
  void PeriodicUpdate();

  TaskQueueBase* const worker_queue_;
  RTC_NO_UNIQUE_ADDRESS SequenceChecker pacer_checker_;
  const uint32_t ssrc_;
  const absl::optional<uint32_t> rtx_ssrc_;
  const absl::optional<uint32_t> flexfec_ssrc_;
  const bool populate_network2_timestamp_;
  const bool send_side_bwe_with_overhead_;
  const bool deferred_sequencing_;
  Clock* const clock_;
  RtpPacketHistory* const packet_history_;
  Transport* const transport_;
  RtcEventLog* const event_log_;
#if BWE_TEST_LOGGING_COMPILE_TIME_ENABLE
  const bool is_audio_;
#endif
  const bool need_rtp_packet_infos_;
  VideoFecGenerator* const fec_generator_ RTC_GUARDED_BY(pacer_checker_);
  absl::optional<uint16_t> last_sent_seq_ RTC_GUARDED_BY(pacer_checker_);
  absl::optional<uint16_t> last_sent_rtx_seq_ RTC_GUARDED_BY(pacer_checker_);

  TransportFeedbackObserver* const transport_feedback_observer_;
  SendSideDelayObserver* const send_side_delay_observer_;
  SendPacketObserver* const send_packet_observer_;
  StreamDataCountersCallback* const rtp_stats_callback_;
  BitrateStatisticsObserver* const bitrate_callback_;

  mutable Mutex lock_;
  bool media_has_been_sent_ RTC_GUARDED_BY(pacer_checker_);
  bool force_part_of_allocation_ RTC_GUARDED_BY(lock_);
  uint32_t timestamp_offset_ RTC_GUARDED_BY(worker_queue_);

  SendDelayMap send_delays_ RTC_GUARDED_BY(lock_);
  SendDelayMap::const_iterator max_delay_it_ RTC_GUARDED_BY(lock_);
  // The sum of delays over a kSendSideDelayWindowMs sliding window.
  int64_t sum_delays_ms_ RTC_GUARDED_BY(lock_);
  uint64_t total_packet_send_delay_ms_ RTC_GUARDED_BY(lock_);
  StreamDataCounters rtp_stats_ RTC_GUARDED_BY(lock_);
  StreamDataCounters rtx_rtp_stats_ RTC_GUARDED_BY(lock_);
  // One element per value in RtpPacketMediaType, with index matching value.
  std::vector<RateStatistics> send_rates_ RTC_GUARDED_BY(lock_);
  absl::optional<std::pair<FecProtectionParams, FecProtectionParams>>
      pending_fec_params_ RTC_GUARDED_BY(lock_);

  // Maps sent packets' sequence numbers to a tuple consisting of:
  // 1. The timestamp, without the randomizing offset mandated by the RFC.
  // 2. Whether the packet was the first in its frame.
  // 3. Whether the packet was the last in its frame.
  const std::unique_ptr<RtpSequenceNumberMap> rtp_sequence_number_map_
      RTC_GUARDED_BY(worker_queue_);
  RepeatingTaskHandle update_task_ RTC_GUARDED_BY(worker_queue_);
  ScopedTaskSafety task_safety_;
};

}  // namespace webrtc

#endif  // MODULES_RTP_RTCP_SOURCE_RTP_SENDER_EGRESS_H_
