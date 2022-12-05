#include <iostream>
#include <cassert>
#include <sstream>
#include <string>
#include <sys/stat.h>
#include "MpegTsDemux.hpp"

using namespace std;

static bool file_exists(const std::string &filename) {
    struct stat buffer{};
    return (stat(filename.c_str(), &buffer) == 0);
}

/// ////////////////////////////////////////////////////////////////////////////
Stream::Stream(const std::string &filename, PID id, PID pid, std::stringstream &errp)
        : m_file(nullptr), m_id(id), m_pid(pid) {
    assert(!filename.empty());
    string final_file_name = filename;
    if (file_exists(filename)) {
        size_t n = final_file_name.find_last_of('.');
        if (n != string::npos) {
            final_file_name.insert(n, "_" + to_string(pid) + "_" + to_string(id));
        } else {
            final_file_name += ("_" + to_string(pid) + "_" + to_string(id));
        }
    }
    m_file = fopen(final_file_name.c_str(), "wb");
    if (!m_file) {
        errp << "[ERROR]: STREAM=" << m_id << " PID=" << m_pid << " : can't open file \'"
             << filename << "\'" << endl;
    }
}

Stream::~Stream() {
    if (m_file) fclose(m_file);
}

/// ////////////////////////////////////////////////////////////////////////////
void Stream::Write(Packet::const_iterator b, Packet::const_iterator e, std::stringstream &errp) {
    if (!m_file) return; // Failed to create stream. Exit quietly

    while (b != e) {
        int r = fputc(*b++, m_file);
        if (r == EOF) {
            errp << "[ERROR]: STREAM=" << m_id << " PID=" << m_pid << " Write failed." << endl;
            fclose(m_file);
            m_file = nullptr;
        }
    }
}
/// ////////////////////////////////////////////////////////////////////////////

/// ////////////////////////////////////////////////////////////////////////////
MpegTsDemuxer::MpegTsDemuxer(bool info)
        : m_pnum(0), m_info(info) {
    // Ignore all packets except PES by default, except when requested.
    if (m_info) {
        // List know PIDs in the filters.
        m_filters.insert(make_pair(MPEGTS_PID_NULL, DEMUXER_EVENT_NIL));
        m_filters.insert(make_pair(MPEGTS_PID_PAT, DEMUXER_EVENT_PAT));
    }
}

MpegTsDemuxer::~MpegTsDemuxer() {
    for (auto &m_stream: m_streams) {
        // clean up
        delete m_stream.second;
    }
}

/// ////////////////////////////////////////////////////////////////////////////
bool MpegTsDemuxer::DecodePacket(const Packet &packet, const std::string &output) {
    ++m_pnum;

    Packet::const_iterator p = packet.begin(), e = packet.end();
    PacketHeader header{};
    if (!ReadHeader(p, e, header))
        return false;

    PID &id = header.id;
    //wrn << "DecodePacket: PID=" << id << endl;

    // Lookup this ID in our filters
    auto f = m_filters.find(id);
    if (f != m_filters.end()) {
        DemuxerEvents E = (*f).second;
        switch (E) {
            case DEMUXER_EVENT_NIL:
                return true;
            case DEMUXER_EVENT_PAT:
                if (!ReadPAT(p, e))
                    return false;
                break;
            case DEMUXER_EVENT_PMT:
                if (!ReadPMT(p, e, output))
                    return false;
                break;
            case DEMUXER_EVENT_PES:
                if (!ReadPES(p, e, header, id, output))
                    return false;
                break;
            default:
                wrn << "[WARNING]: PID=" << id << " filtered but not handled." << endl;
                break;
        }
    } else {
        // Check if orphaned PES with no program.
        if (CheckPES(p, e, header)) {
            if (!ReadPES(p, e, header, id, output))
                return false;
        }
    }
    return true;
}

/// ////////////////////////////////////////////////////////////////////////////
bool
MpegTsDemuxer::ReadHeader(Packet::const_iterator &p, Packet::const_iterator, PacketHeader &header) {
    if (*p != MPEGTS_SYNC_BYTE) {
        errp << "[ERROR]: PKT#" << m_pnum << " Sync byte not found." << endl;
        return false;
    }
    p++;

    header.tei = static_cast<bool> (*p & 0x80);
    header.pusi = static_cast<bool> (*p & 0x40);
    header.tp = static_cast<bool> (*p & 0x20);

    header.id = PID((p[0] & 0x1F) << 8) | PID(p[1]);
    p += 2;

    header.afc = (((*p & 0x30) >> 4) == 0x2) || (((*p & 0x30) >> 4) == 0x3);
    header.payload = (((*p & 0x30) >> 4) == 0x1) || (((*p & 0x30) >> 4) == 0x3);
    header.cc = *p & 0xF;
    p++;

    // Greedy, just escape the adaptation control field
    if (header.afc) {
        const uint8_t l = *p++;
        p += l;
    }

    return true;
}

/// ////////////////////////////////////////////////////////////////////////////
bool MpegTsDemuxer::ReadPAT(Packet::const_iterator &p, Packet::const_iterator e) {
    const uint8_t pointer = *p++;
    p += pointer; // Jump to table at pointer offset

    bool done = false; // Must get at least 1 Program

    // Tables can be repeated.
    while (p < e) {
        const uint8_t id = *p++;
        if (id == MPEGTS_STUFFING_BYTE) {
            if (!done) {
                errp << "[ERROR]: PKT#" << m_pnum << " Empty PAT packet." << endl;
                return false;
            }
            // we are done, happy return
            return true;
        } else if (id == MPEGTS_TABLE_NIL) {
            if (!done) {
                errp << "[ERROR]: PKT#" << m_pnum << " PAT packet has no associated PSI." << endl;
                return false;
            }
            return true;
        } else if (id != MPEGTS_TABLE_PAS) {
            errp << "[ERROR]: PKT#" << m_pnum << " Expected PAT SECTION=" << MPEGTS_TABLE_PAS
                 << " found " << id << endl;
            return false;
        }

        const bool ssi = static_cast<bool> (*p & 0x80); // PAT/PMT/CAT == 1
        if (!ssi) {
            errp << "[ERROR]: PKT#" << m_pnum << " PAT section syntax indicator not set." << endl;
            return false;
        }

        const bool pb = static_cast<bool> (*p & 0x40); // PAT/PMT/CAT == 0
        if (pb) {
            errp << "[ERROR]: PKT#" << m_pnum << " PAT private bit is set." << endl;
            return false;
        }

        if (((*p & 0x30) >> 4) != 0x03) {
            errp << "[ERROR]: PKT#" << m_pnum << " PAT PSI reserved bits not set." << endl;
            return false;
        }

        if (p + 1 >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " PAT PSI not enough data." << endl;
            return false;
        }
        const uint16_t slength = uint16_t((p[0] & 0x03) << 8) | uint16_t(p[1]);
        p += 2;
        if (p + slength >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " PAT bad section length." << endl;
            return false;
        }

        PacketSection section{};
        if (!ReadSection(p, e, section))
            return false;

        // Read PAT, can be repeated
        if (!ReadPrograms(p, e))
            return false;

        // End of the section
        //uint32_t crc = uint32_t(p[0]<<24) | uint32_t(p[1]<<16) | uint32_t(p[2]<<8) | uint32_t(p[3]);
        //wrn << "CRC: " << hex << crc << dec << endl;
        p += 4;
        done = true;
    }

    return done;
}

/// ////////////////////////////////////////////////////////////////////////////
/// Read PAT tables
bool MpegTsDemuxer::ReadPrograms(Packet::const_iterator &p, Packet::const_iterator e) {
    bool done = false;
    // Read PAT table till end of packet (CRC)
    while (p + 3 < e) {
        if (p[4] == MPEGTS_STUFFING_BYTE) {
            // Read PAT table till end of packet (CRC+STUFFING)
            break;
        }

        uint16_t pnum = uint16_t(p[0] << 8) | uint16_t(p[1]);
        p += 2;

        if (((*p & 0xE0) >> 5) != 0x07) {
            errp << "[ERROR]: PKT#" << m_pnum << " PAT reserved bits not set." << endl;
            return false;
        }
        uint16_t pmid = uint16_t((p[0] & 0x1F) << 8) | uint16_t(p[1]);
        p += 2;

        RegisterProgram(pnum, pmid);
        //wrn << "PNUM: " << pnum << " PMAP PID: " << pmid << endl;
        done = true;
    }

    if (!done) {
        errp << "[ERROR]: PKT#" << m_pnum << " PAT can't find programs." << endl;
    }
    return done;
}

/// ////////////////////////////////////////////////////////////////////////////
bool MpegTsDemuxer::ReadPMT(Packet::const_iterator &p, Packet::const_iterator e,
                            const std::string &output) {
    /// FIXME: Similar to ReadPAT, can it be grouped together.

    const uint8_t pointer = *p++;
    p += pointer; // Jump to table at pointer offset

    bool done = false;

    // Tables can be repeated.
    while (p < e) {
        if (p[0] == MPEGTS_STUFFING_BYTE) {
            if (!done) {
                errp << "[ERROR]: PKT#" << m_pnum << " No data in PMT table." << endl;
                return false;
            }
            return true;
        }
        const uint8_t id = *p++;
        if (id == MPEGTS_TABLE_NIL) {
            if (!done) {
                errp << "[ERROR]: PKT#" << m_pnum << " PMT packet has no associated PSI." << endl;
                return false;
            }
            return true;
        } else if (id != MPEGTS_TABLE_PMS) {
            // can happen! Just ignore.
            wrn << "[WARNING]: PKT#" << m_pnum << "Expected PMT SECTION=" << MPEGTS_TABLE_PMS
                << " found " << id << endl;
            return true;
        }

        const bool ssi = static_cast<bool> (*p & 0x80); // PAT/PMT/CAT == 1
        if (!ssi) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT section syntax indicator not set." << endl;
            return false;
        }

        const bool pb = static_cast<bool> (*p & 0x40); // PAT/PMT/CAT == 0
        if (pb) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT private bit is set." << endl;
            return false;
        }

        if (((*p & 0x30) >> 4) != 0x03) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT PSI reserved bits not set." << endl;
            return false;
        }

        if (p + 1 >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " Not enough data in PMT table." << endl;
            return false;
        }
        const uint16_t slength = uint16_t((p[0] & 0x03) << 10) | uint16_t(p[1]);
        p += 2;
        if (p + slength >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT bad section length." << endl;
            return false;
        }

        PacketSection section{};
        if (!ReadSection(p, e, section))
            return false;

        // Get the program this PMT referring to
        Programs::const_iterator prog = m_programs.find(section.id);
        if (prog == m_programs.end()) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT references non existing PROGRAM="
                 << section.id << endl;
            return false;
        }

        if (((*p & 0xE0) >> 5) != 0x07) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT reserved bits not set." << endl;
            return false;
        }

        if (p + 1 >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " Not enough data in PMT table." << endl;
            return false;
        }
        //PID pcrid = uint16_t((p[0]&0x1F)<<8) | uint16_t(p[1]); // clock frequency
        p += 2;

        // expect PES packet with clock frequency
        //m_filters.insert(make_pair(pcrid, DEMUXER_EVENT_PCR));

        if (((*p & 0xF0) >> 4) != 0x0F) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT reserved bits not set." << endl;
            return false;
        }

        // program descriptors
        if (p + 1 >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " Not enough data in PMT table." << endl;
            return false;
        }
        const uint16_t pinfol = uint16_t((p[0] & 0x03) << 8) | uint16_t(p[1]);
        p += 2;

        if (p + pinfol >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " PMT bad program info length." << endl;
            return false;
        }
        p += pinfol;

        // read data stream info
        if (!ReadESD(p, e, (*prog).second, output))
            return false;

        // End of the section
        //uint32_t crc = uint32_t(p[0]<<24) | uint32_t(p[1]<<16) | uint32_t(p[2]<<8) | uint32_t(p[3]);
        p += 4;
        //wrn << "CRC: " << hex << crc << dec << endl;
        done = true;
    }

    return done;
}

/// ////////////////////////////////////////////////////////////////////////////
/// Reads PES packet
bool
MpegTsDemuxer::ReadPES(Packet::const_iterator &p, Packet::const_iterator e, PacketHeader &header,
                       PID id, const std::string &output) {
    if (header.pusi) {
        /// PES PSI
        if (p[0] == 0x0 && p[1] == 0x0 && p[2] == 0x01) {
            p += 3;
            const uint8_t sidx = *p++;
            const bool video = MPEGTS_VIDEO_STREAM(sidx);
            const bool expected = MPEGTS_AUDIO_STREAM(sidx) || video;
            if (!expected) {
                errp << "[ERROR]: PKT#" << m_pnum << " Expected audio/video packets only. FOUND="
                     << hex << uint16_t(sidx) << dec << endl;
                return false;
            }

            auto s = m_streams.find(id);
            if (s == m_streams.end()) {
                if (!RegisterStream(id, Program(), video, output))
                    return false;
                s = m_streams.find(id);
            }
            Stream *S = (*s).second;

            // escape packet length
            p += 2;

            // The rest of the header is of variable length.
            // but we know that there are at least 3 bytes. The third is the length of the optional fields followed by a byte that stores the
            p += 2;


            const uint8_t pl = *p++;
            if (p + pl >= e) {
                errp << "[ERROR]: PKT#" << m_pnum << " PES Invalid length." << m_pnum << endl;
                return false;
            }
            p += pl;

            // write to stream
            S->Write(p, e, errp);
        } else {
            errp << "[ERROR]: PKT#" << m_pnum << " Expected PES start sequence." << endl;
            return false;
        }
    } else {
        auto s = m_streams.find(id);
        if (s == m_streams.end()) {
            errp << "[ERROR]: PKT#" << m_pnum << " Invalid STREAM=" << id << endl;
            return false;
        }
        Stream *S = (*s).second;
        S->Write(p, e, errp);
    }
    return true;
}

////////////////////////////////////////////////////////////////////////////
bool
MpegTsDemuxer::CheckPES(Packet::const_iterator &p, Packet::const_iterator e, PacketHeader &header) {
    if (header.pusi) {
        return (p[0] == 0x0 && p[1] == 0x0 && p[2] == 0x01);
    }
    return false;
}

/// ////////////////////////////////////////////////////////////////////////////
bool MpegTsDemuxer::ReadSection(Packet::const_iterator &p, Packet::const_iterator e,
                                PacketSection &section) {
    if (p + 4 >= e) {
        errp << "[ERROR]: PKT#" << m_pnum << " not enough data in PSI section." << endl;
        return false;
    }

    section.id = uint16_t(p[0] << 8) | uint16_t(p[1]);
    p += 2;

    if (((*p & 0xC0) >> 6) != 0x03) {
        errp << "[ERROR]: PKT#" << m_pnum << " Section reserved bits not set." << endl;
        return false;
    }
    section.version = (*p & 0x3E) >> 1;
    section.cni = static_cast<bool> (*p++ & 0x1);

    section.sn = *p++;
    section.lsn = *p++;
    return true;
}

/// ////////////////////////////////////////////////////////////////////////////
void MpegTsDemuxer::RegisterProgram(PID id, PID pid) {
    assert(id != MPEGTS_PID_NULL);

    auto p = m_programs.find(id);
    if (p == m_programs.end()) {
        wrn << "[INFO]: PKT#" << m_pnum << " PROGRAM=" << id << " PMT=" << pid << endl;

        Program P;
        P.id = id;
        P.pid = pid;
        m_programs.insert(make_pair(id, P));

        // expect packet for PMT
        m_filters.insert(make_pair(pid, DEMUXER_EVENT_PMT));
    } else {
        if (p->second.pid != pid) {
            // Support updating program information
            wrn << "[WARNING]: PKT#" << m_pnum << " PROGRAM=" << id << " PMT=" << p->second.pid
                << " appeared with PMT=" << pid << endl;
            p->second.pid = pid;

            // expect packet for PMT
            m_filters.insert(make_pair(pid, DEMUXER_EVENT_PMT));
        }
    }
}

/// ////////////////////////////////////////////////////////////////////////////
bool
MpegTsDemuxer::ReadESD(Packet::const_iterator &p, Packet::const_iterator e, const Program &prog,
                       const std::string &output) {
    bool found = false;
    while (p + 3 < e) {
        // watch out not to read CRC
        if (p[4] == MPEGTS_STUFFING_BYTE) {
            // Oh, we hit it. Okay happy return.
            if (!found) {
                errp << "[ERROR]: PKT#" << m_pnum << " Empty ESD section." << endl;
                return false;
            }
            return true;
        }

        const uint8_t st = *p++;
        if (((*p & 0xE0) >> 5) != 0x07) {
            errp << "[ERROR]: PKT#" << m_pnum << " ESD reserved bits not set." << endl;
            return false;
        }

        // See what is the stream type
        const bool video = MPEGTS_VIDEO_STREAM_TYPE(st);
        const bool needed = MPEGTS_AUDIO_STREAM_TYPE(st) || video;

        PID id = PID((p[0] & 0x1F) << 8) | PID(p[1]);
        p += 2;

        if (((*p & 0xF0) >> 4) != 0x0F) {
            errp << "[ERROR]: PKT#" << m_pnum << "ESD reserved bits not set." << endl;
            return false;
        }

        const uint16_t esil = uint16_t((p[0] & 0x03) << 8) | uint16_t(p[1]);
        p += 2;

        if (p + esil >= e) {
            errp << "[ERROR]: PKT#" << m_pnum << " ESD invalid info length." << endl;
            return false;
        }
        p += esil;

        // Add video/audio streams only
        if (needed) {
            if (!RegisterStream(id, prog, video, output))
                return false;
            found = true; // we found a stream
        }
    }
    return true;
}

/// ////////////////////////////////////////////////////////////////////////////
bool
MpegTsDemuxer::RegisterStream(PID id, const Program &prog, bool video, const std::string &output) {
    auto s = m_streams.find(id);
    if (s == m_streams.end()) {
        auto *S = new Stream(output, id, prog.id, errp);
        m_streams.insert(make_pair(id, S));
        if (m_info)
            wrn << "[INFO]: PKT#" << m_pnum << " STREAM=" << id << " PROGRAM=" << prog.id
                << " TYPE=" << (video ? "VIDEO" : "AUDIO") << endl;
        else
            wrn << "[INFO]: PKT#" << m_pnum << " STREAM=" << id << " TYPE="
                << (video ? "VIDEO" : "AUDIO") << endl;
        // expect packet for stream
        m_filters.insert(make_pair(id, DEMUXER_EVENT_PES));
    } else {
        if ((*s).second->GetPId() != prog.id && prog.id != MPEGTS_PID_NULL) {
            // Support updating stream information.
            wrn << "[WARNING]: PKT#" << m_pnum << " STREAM=" << id << " PROGRAM="
                << (*s).second->GetPId() << " appeared in PROGRAM=" << prog.id << endl;
            (*s).second->SetPId(prog.id);
        }
    }
    return true;
}
