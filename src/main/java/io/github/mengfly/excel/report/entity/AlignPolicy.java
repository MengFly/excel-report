package io.github.mengfly.excel.report.entity;

public enum AlignPolicy {

    START {
        @Override
        public int getPoint(int totalSize, int size) {
            return 0;
        }
    },
    CENTER {
        @Override
        public int getPoint(int totalSize, int size) {
            return (totalSize - size) / 2;
        }
    },
    END {
        @Override
        public int getPoint(int totalSize, int size) {
            return totalSize - size;
        }
    };


    public abstract int getPoint(int totalSize, int size);

}
