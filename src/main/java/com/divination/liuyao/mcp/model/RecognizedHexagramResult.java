package com.divination.liuyao.mcp.model;

import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.Hexagram;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecognizedHexagramResult {

    private String inputImageUrl;

    private String sourceImageUrl;

    private String imageUrl;

    private Prediction prediction;

    private Hexagram hexagram;

    private String hexagramText;

    private boolean imageRenderingImplemented;

    private String imageRenderingStatus;
}
