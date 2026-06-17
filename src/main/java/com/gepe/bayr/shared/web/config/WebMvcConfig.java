//package com.gepe.bayr.shared.web.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
//import org.springframework.web.method.support.HandlerMethodArgumentResolver;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.util.List;
//
///**
// * Konfigurasi kustom untuk memperluas kemampuan Spring MVC Argument Resolvers.
// * Kelas ini sangat krusial agar satu endpoint Controller bisa menerima berbagai macam jenis format request.
// */
//@Configuration
//public class WebMvcConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
//        /*
//         * 💡 KENAPA KODE INI ADA? (PENTING JANGAN DIHAPUS!)
//         * * Masalah:
//         * Di Controller, kita menghapus anotasi `@RequestBody` agar endpoint bisa menerima data dari
//         * format 'multipart/form-data' (Form Data) dan 'application/x-www-form-urlencoded' (Form URL Encoded).
//         * Namun, efek sampingnya, Spring jadi kehilangan kemampuan untuk mem-parsing format 'application/json' (JSON).
//         * * Solusi (Kode di bawah ini):
//         * Kita mendaftarkan `ModelAttributeMethodProcessor(true)`. Parameter `true` (useDefaultResolution) memaksa
//         * Spring untuk tetap memperlakukan objek parameter biasa (seperti Record RegisterReq tanpa @RequestBody)
//         * sebagai target resolver data, SEKALIGUS tetap mengizinkan HttpMessageConverter (Jackson) untuk
//         * mencoba membaca data jika request yang masuk berupa JSON.
//         * * Hasil Akhir:
//         * Satu endpoint Controller yang sama (tanpa @RequestBody) sekarang sakti karena bisa mendukung 3 jenis format sekaligus:
//         * 1. JSON (Raw application/json)
//         * 2. x-www-form-urlencoded (Formulir web biasa)
//         * 3. form-data (Formulir biner/multimedia)
//         */
//        resolvers.add(new ModelAttributeMethodProcessor(true));
//    }
//}