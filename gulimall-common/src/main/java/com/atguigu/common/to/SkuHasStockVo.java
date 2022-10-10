package com.atguigu.common.to;

public class SkuHasStockVo {
    private Long skuId;

    private Boolean hasStock;

    public SkuHasStockVo() {
    }

    public SkuHasStockVo(Long skuId, Boolean hasStock) {
        this.skuId = skuId;
        this.hasStock = hasStock;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Boolean getHasStock() {
        return hasStock;
    }

    public void setHasStock(Boolean hasStock) {
        this.hasStock = hasStock;
    }

    @Override
    public String toString() {
        return "SkuHasStockVo{" +
                "skuId=" + skuId +
                ", hasStock=" + hasStock +
                '}';
    }
}
