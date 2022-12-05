/// ////////////////////////////////////////////////////////////////////////////
/// Defines Transport Stream Structures.
/// /////////////////////////////////////////////////////////////////////////////
/// Transport Stream Structure
/// A packet is the basic unit of data. Transport stream is merely
/// a sequence of packets, without any global header.
/// Each packet starts with a sync byte and a header, the rest of
/// the packet consists of payload.
/// Packets are 188 bytes in length, but communication medium may
/// add additional information.
///
/// References: https://www.en.wikipedia.org/wiki/MPEG_transport_stream
///		http://www.itu.int/rec/T-REC-H.222.0
///
///
#ifndef INCLUDED_MPEGTS_HPP
#define INCLUDED_MPEGTS_HPP

#include <cstdint>

/// Default Packet Size, the standard packet size is only supported for now.
/// Other packet sizes can exists, with more bytes up to 204. This can be easily
/// detected using buffered look-ahead stream reading to probe the bit stream for the next
/// sync byte if not found in byte #189.
#define PACKET_SIZE 188

/// ////////////////////////////////////////////////////////////////////////////
/// MPEG TS Packet
struct Packet {
    using value_type = uint8_t;
    using iterator = uint8_t *;
    using const_iterator = const uint8_t *;

    iterator begin() { return &m_data[0]; }

    const_iterator begin() const { return &m_data[0]; }

    iterator end() { return &m_data[PACKET_SIZE - 1] + 1; }

    const_iterator end() const { return &m_data[PACKET_SIZE - 1] + 1; }

    uint8_t m_data[PACKET_SIZE];
};

/// Sync Byte
#define MPEGTS_SYNC_BYTE 0x47

/// Packet Identifier
using PID = uint16_t;

/// Stuffing Byte
#define MPEGTS_STUFFING_BYTE 0xFF

/// Packet Identifier (Not Complete)
#define MPEGTS_PID_PAT 0x0000
#define MPEGTS_PID_CAT 0x0001
#define MPEGTS_PID_TSDT 0x0002
#define MPEGTS_PID_IPMP 0x0003
#define MPEGTS_PID_NULL 0x1FFF

/// Table Identifiers (Not Complete)
#define MPEGTS_TABLE_PAS 0x00 /// Program Association section contains a directory listing of all program map tables.
#define MPEGTS_TABLE_CAS 0x01 /// Conditional Access section contains a directory listing of all EMM streams.
#define MPEGTS_TABLE_PMS 0x02 /// Program Map section contains a directory listing of all elementary streams. (PES)
#define MPEGTS_TABLE_TDS 0x03 /// Transport Stream Description section.
#define MPEGTS_TABLE_MDS 0x06 /// Metadata section.
#define MPEGTS_TABLE_NIL 0xFF /// End of section.

/// Stream IDS
// TODO: 0xBD is a private stream can hold audio data. Not sure should I include it or not some samples use it.
#define MPEGTS_AUDIO_STREAM(id)  ((id)==0xBD || ((id) >= 0xC0 && (id) <= 0xDF))
#define MPEGTS_VIDEO_STREAM(id)  ((id) >= 0xE0 && (id) <= 0xEF)

/// Stream Types (Not Complete)
#define MPEGTS_STREAM_VIDEO_MPEG1 0x01
#define MPEGTS_STREAM_VIDEO_MPEG2 0x02
#define MPEGTS_STREAM_VIDEO_MPEG4 0x10
#define MPEGTS_STREAM_VIDEO_H264  0x1B
#define MPEGTS_STREAM_AUDIO_MPEG1 0x03
#define MPEGTS_STREAM_AUDIO_MPEG2 0x04
#define MPEGTS_STREAM_AUDIO_ACC 0x0F
#define MPEGTS_STREAM_AUDIO_AC3 0x81

#define MPEGTS_AUDIO_STREAM_TYPE(st) ((st)==MPEGTS_STREAM_AUDIO_MPEG1 || \
                                      (st)==MPEGTS_STREAM_AUDIO_MPEG2 || \
                                      (st)==MPEGTS_STREAM_AUDIO_ACC   || \
                                      (st)==MPEGTS_STREAM_AUDIO_AC3)
#define MPEGTS_VIDEO_STREAM_TYPE(st) ((st)==MPEGTS_STREAM_VIDEO_MPEG1 || \
                                      (st)==MPEGTS_STREAM_VIDEO_MPEG2 || \
                                      (st)==MPEGTS_STREAM_VIDEO_MPEG4 || \
                                      (st)==MPEGTS_STREAM_VIDEO_H264)

/// ///////////////////////////////////////////////////////////////////////////
/// Program
/// MPEG-TS has the concept of programs. Multiple/different programs can share
/// the stream in time division multiplexing manner.
/// Program packet PAT signals the start of a certain program data in MPEG-TS.
struct Program {
    PID id; /// Program ID
    PID pid; /// Id of the PMT table for this program
    //std::set<PID> pids; /// Packets associated with the program [UNUSED]

    bool operator==(const Program &p) { return id == p.id; }

    bool operator!=(const Program &p) { return id != p.id; }

    friend bool operator<(const Program &p1, const Program &p2);

    Program() : id(MPEGTS_PID_NULL), pid(MPEGTS_PID_NULL) {
        // default to NULL packet
    }
};

inline bool operator<(const Program &p1, const Program &p2) {
    return p1.id < p2.id;
}

/// ///////////////////////////////////////////////////////////////////////////
/// Packet Header
struct PacketHeader {
    bool tei; /// Transport Error Indicator
    bool pusi; /// Payload Uint Start Indicator
    bool tp; /// Transport priority
    PID id; /// Packet ID
    // Transport Scrambling Control goes here
    bool afc; /// Adaptation field
    bool payload; /// Has payload
    uint8_t cc; /// Continuity counter
};

/// ///////////////////////////////////////////////////////////////////////////
/// Packet Section
struct PacketSection {
    PID id; /// Table ID extension
    uint8_t version;
    bool cni; /// Current/Next indicator
    uint8_t sn; /// Section number
    uint8_t lsn; /// Last section number
};

#endif
