#include <cassert>
#include <cstdlib>
#include <iostream>
#include <sstream>
#include "MpegTsBitStream.hpp"

using namespace std;

MpegTsBitStream::MpegTsBitStream(const string &filename) : m_file(nullptr), m_eof(false),
                                                           m_bad(true) {
    assert(!filename.empty());

    m_file = fopen(filename.c_str(), "rb");
    if (!m_file) {
        errp << "[ERROR]: can't open file \'" << filename << "\'" << endl;
    } else {
        m_bad = false;
    }
}

MpegTsBitStream::~MpegTsBitStream() {
    if (m_file) fclose(m_file);
}

bool MpegTsBitStream::GetPacket(Packet &packet) {
    // Fast exit
    if (!m_file || !good()) return false;

    Packet::iterator p = packet.begin();

    size_t count = 1;
    int C = fgetc(m_file);

    if (C == EOF) {
        /// No More packets in stream
        m_eof = true;
        return false;
    }

    auto c = static_cast<uint8_t> (C);
    if (c != MPEGTS_SYNC_BYTE) {
        errp << "[ERROR]: Sync byte not found." << endl;
        m_bad = true;
        return false;
    }

    *p++ = c;
    while (p != packet.end()) {
        C = fgetc(m_file);
        if (C == EOF) {
            m_eof = true;
            break;
        }
        c = static_cast<uint8_t> (C);
        *p++ = c;
        ++count;
    }

    if (count != PACKET_SIZE) {
        errp << "[ERROR]: Expected " << PACKET_SIZE << " bytes, found " << count << endl;
        m_bad = true;
        return false;
    }

    return true;
}

