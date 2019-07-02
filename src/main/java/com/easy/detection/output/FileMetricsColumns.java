/**
 *
 */
package com.easy.detection.output;

import com.easy.detection.data.Context;
import com.easy.detection.data.File;

/**
 * Columns and value providers for the &quot;files&quot; CSV output file
 *
 * @author wfenske
 */
public enum FileMetricsColumns implements CsvColumnValueProvider<com.easy.detection.data.File, Context> {
    File {
        @Override
        public String csvColumnValue(File file, Context ctx) {
            return file.FilePathForDisplay();
        }
    },
    AFSmell {
        @Override
        public Float csvColumnValue(File file, Context ctx) {
            // calculate smell values
            // Loac/Loc * #FeatLocs
            float featLocSmell = (float) LocationSmell.csvColumnValue(file, ctx);
            // #Constants/#FeatLocs
            float featConstSmell = (float) ConstantsSmell.csvColumnValue(file, ctx);
            // Loac/Loc * #FeatLocs
            float nestSumSmell = (float) NestingSmell.csvColumnValue(file, ctx);
            float aggregated = (featLocSmell + featConstSmell + nestSumSmell);
            return aggregated;
        }
    },
    LocationSmell {
        @Override
        public Float csvColumnValue(File file, Context ctx) {
            // calculate smell values
            // Loac/Loc * #FeatLocs
            float featLocSmell = ctx.config.File_LoacToLocRatio_Weight
                    * (((float) file.GetLinesOfAnnotatedCode() / (float) file.loc) * file.numberOfFeatureLocations);
            return featLocSmell;
        }
    },
    ConstantsSmell {
        @Override
        public Float csvColumnValue(File file, Context ctx) {
            // calculate smell values
            // Loac/Loc * #FeatLocs
            // #Constants/#FeatLocs
            float featConstSmell = ctx.config.File_NumberOfFeatureConstants_Weight
                    * ((float) file.GetFeatureConstantCount() / (float) file.numberOfFeatureLocations);
            return featConstSmell;
        }
    },
    NestingSmell {
        @Override
        public Float csvColumnValue(File file, Context ctx) {
            // Loac/Loc * #FeatLocs
            float nestSumSmell = ctx.config.Method_NestingSum_Weight
                    * ((float) file.nestingSum / (float) file.numberOfFeatureLocations);
            return nestSumSmell;
        }
    },
    LOC {
        @Override
        public Integer csvColumnValue(File file, Context ctx) {
            return file.loc;
        }
    },
    LOAC {
        @Override
        public Integer csvColumnValue(File file, Context ctx) {
            return file.GetLinesOfAnnotatedCode();
        }
    },
    LOFC {
        @Override
        public Integer csvColumnValue(File file, Context ctx) {
            return file.lofc;
        }
    },
    NOFC_Dup {
        @Override
        public Integer csvColumnValue(File file, Context ctx) {
            return file.GetFeatureConstantCount();
        }
    },
    NOFC_NonDup {
        @Override
        public Integer csvColumnValue(File file, Context ctx) {
            return file.numberFeatureConstantsNonDup;
        }
    },
    NOFL {
        @Override
        public Integer csvColumnValue(File file, Context ctx) {
            return file.numberOfFeatureLocations;
        }
    },
    NONEST {
        @Override
        public Integer csvColumnValue(File file, Context ctx) {
            return file.nestingSum;
        }
    };
}
