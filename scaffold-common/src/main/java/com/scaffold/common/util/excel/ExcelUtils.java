package com.scaffold.common.util.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 基于 EasyExcel 封装的 Excel 导入导出工具（common 工具层，非 starter：无装配语义，
 * easyexcel 为 optional 依赖，不用 Excel 能力的模块零传递成本）。
 *
 * <p>导出一行代码完成下载（文件名自动 URL 编码）；导入走逐批回调，
 * 业务在 listener 中校验并收集结果——大文件按千行分批，避免一次性驻留内存。</p>
 *
 * @author lianghang
 */
public final class ExcelUtils {

    private ExcelUtils() {
    }

    /** 导出下载（文件名自动 URL 编码） */
    public static <T> void write(HttpServletResponse response, String fileName,
                                 Class<T> clazz, List<T> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment;filename*=utf-8''" + encodedName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), clazz)
                .sheet(fileName)
                .doWrite(rows);
    }

    /** 读取（逐行回调，业务在 listener 中校验并收集结果） */
    public static <T> void read(java.io.InputStream inputStream, Class<T> clazz,
                                RowListener<T> listener) {
        EasyExcel.read(inputStream, clazz, new AnalysisEventListener<T>() {
            private final List<T> cached = new java.util.ArrayList<>();
            private int row = 0;

            @Override
            public void invoke(T data, AnalysisContext context) {
                cached.add(data);
                if (cached.size() >= 1000) {
                    listener.onBatch(cached, ++row);
                    cached.clear();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                if (!cached.isEmpty()) {
                    listener.onBatch(cached, ++row);
                }
                listener.onComplete();
            }
        }).sheet().doRead();
    }

    /** 逐批处理回调 */
    public interface RowListener<T> {
        void onBatch(List<T> batch, int batchNo);

        void onComplete();
    }
}
