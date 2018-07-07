package com.m.mediax.core.codec.flv;

import java.nio.ByteBuffer;

import org.apache.commons.codec.Decoder;
import org.apache.commons.codec.DecoderException;

import com.m.mediax.core.codec.amf.AMF0Decoder;
import com.m.mediax.core.codec.amf.AMFArray;
import com.m.mediax.core.codec.flv.tag.AudioTag;
import com.m.mediax.core.codec.flv.tag.FlvHeader;
import com.m.mediax.core.codec.flv.tag.MetaDataTag;
import com.m.mediax.core.codec.flv.tag.Tag;
import com.m.mediax.core.codec.flv.tag.VideoTag;
import com.m.mediax.core.utils.ByteUtils;

public class TagDecoder implements Decoder {

    public FlvHeader decodeFlvHead(ByteBuffer bytes) {
        String typeFlag = ByteUtils.read3cc(bytes);
        if (!"flv".equalsIgnoreCase(typeFlag)) {
            throw new IllegalArgumentException("It's not flv file");
        }

        int version = ByteUtils.readUInt8(bytes);
        int flags = ByteUtils.readUInt8(bytes);
        boolean hasAudio = (flags & 0xF0) > 0;
        boolean hasVideo = (flags & 0x0F) > 0;

        return new FlvHeader(version, hasVideo, hasAudio, bytes.slice());
    }
    
    @Override
    public Tag decode(Object source) throws DecoderException {
        if (source instanceof ByteBuffer) {
            return decodeTag((ByteBuffer)source);
        }

        throw new DecoderException("unsupported [" + source + "]");
    }

    public Tag decodeTag(ByteBuffer frameTag) throws DecoderException {
        int tagType = typeOf(frameTag);

        switch (tagType) {
            case FlvConsts.TAGTYPE_VIDEO:
                return decodeVideoTag(frameTag);
            case FlvConsts.TAGTYPE_AUDIO:
                return decodeAudioTag(frameTag);
            case FlvConsts.TAGTYPE_SCRIPT:
                return decodeMetadata(frameTag);
            default:
                throw new DecoderException("unsupport tag type [" + tagType + "]");
        }
    }

    public int typeOf(ByteBuffer tag) {
        int position = tag.position();

        try {
            return ByteUtils.readUInt8(tag);
        } finally {
            tag.position(position);
        }
    }

    public AudioTag decodeAudioTag(ByteBuffer bytes) throws DecoderException {
        // tag head
        int type = ByteUtils.readUInt8(bytes);
        int dataSize = ByteUtils.readUInt24(bytes);
        long timestamp = ByteUtils.readUInt24(bytes);
        timestamp |= ByteUtils.readUInt8(bytes) << 24;
        int streamId = ByteUtils.readUInt24(bytes);

        // tag data
        int soundConfig = ByteUtils.readUInt8(bytes);
        int soundFormat = (soundConfig >> 4) & 0xF;
        int soundRate = (soundConfig >> 2) & 0x3;
        int soundSize = (soundConfig >> 1) & 0x1;
        int soundType = (soundConfig >> 0) & 0x1;
        
        AudioTag tag = new AudioTag();
        tag.setSoundFormat(soundFormat);
        tag.setSoundRate(soundRate);
        tag.setSoundSize(soundSize);
        tag.setSoundType(soundType);
        
        tag.setTagType(type);
        tag.setDataSize(dataSize);
        tag.setTimestamp(timestamp);
        tag.setStreamId(streamId);
        return tag;
    }


    public Tag decodeVideoTag(ByteBuffer bytes) {
        // tag head
        int type = ByteUtils.readUInt8(bytes);
        int dataSize = ByteUtils.readUInt24(bytes);
        long timestamp = ByteUtils.readUInt24(bytes);
        timestamp |= ByteUtils.readUInt8(bytes) << 24;
        int streamId = ByteUtils.readUInt24(bytes);

        // tag data
        int frameKey = ByteUtils.readUInt8(bytes);
        int frameType = (frameKey & 0xF0) >> 4;
        int codeId = (frameKey & 0x0F) >> 0;

        VideoTag tag = new VideoTag();
        tag.setTagType(type);
        tag.setDataSize(dataSize);
        tag.setTimestamp(timestamp);
        tag.setStreamId(streamId);
        tag.setFrameType(frameType);
        tag.setCodecId(codeId);
        return tag;
    }

    public MetaDataTag decodeMetadata(ByteBuffer scriptData) throws DecoderException {
        scriptData.position(FlvConsts.TAGHEAD_LENGTH);

        Object[] items = new AMF0Decoder().decode(scriptData);
        if (items.length < 2 || !"onMetaData".equals(items[0])) {
            throw new IllegalArgumentException("不是一个 metadata tag");
        }

        return new MetaDataTag((AMFArray) items[1]);
    }
}
