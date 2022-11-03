package github.kasuminova.balloonserver.httpserver;

import cn.hutool.core.util.StrUtil;

import java.util.List;

public class ContentRanges {
    private final long start;
    private final long end;

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    /**
     * 解析 range 字符串
     * @param rangeContent 要解析的字符串
     * @param type range 的类型：如 "bytes"
     * @param fileRange 文件长度
     */
    public ContentRanges(String rangeContent, String type, long fileRange) {
        if (rangeContent == null) {
            start = 0;
            end = fileRange;
            return;
        }

        String trueRangeContent = StrUtil.removePrefix(rangeContent, type + "=");
        if (trueRangeContent.startsWith("-")) {
            long last = Long.parseLong(trueRangeContent);
            start = fileRange + last;
            end = fileRange;
            return;
        }

        if ("0-0,-1".equals(trueRangeContent)) {
            start = 0;
            end = fileRange;
            return;
        }

        List<String> ranges = StrUtil.split(trueRangeContent, '-', 2);
        start = Long.parseLong(ranges.get(0));

        end = ranges.get(1).isEmpty() ? fileRange : Long.parseLong(ranges.get(1));
    }
}
