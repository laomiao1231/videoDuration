package com.m.mediax.core.codec.flv;

import com.coremedia.iso.IsoFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDuration {

    public static void main(String[] args) throws Exception {
        VideoDuration videoDuration = new VideoDuration();
        System.out.println(videoDuration.flvGetDuration("E:/miao/11.flv"));
        System.out.println(videoDuration.mp4GetDuration("E:/miao/22.mp4"));
    }

    public static long flvGetDuration(String videoPath) throws Exception {
        FlvIterator itr = new FlvIterator(new File(videoPath));
        double timeLength = new Double(0);
        try {
            // metadata
            ByteBuffer meta = itr.next();
            TagDecoder tagDecoder = new TagDecoder();
            FlvMetaData metadata = (FlvMetaData) tagDecoder.decodeTag(meta);
            timeLength = metadata.getDuration();
        } finally {
            itr.close();
        }
        long duration = Math.round(timeLength);
        return duration;
    }

    public static long mp4GetDuration(String videoPath) throws IOException {
        IsoFile isoFile = new IsoFile(videoPath);  // Error:OutOfMemory
        double timeLength = (double)
                isoFile.getMovieBox().getMovieHeaderBox().getDuration() /
                isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
        isoFile.close();
        long duration = Math.round(timeLength);
        return duration;
    }

}
