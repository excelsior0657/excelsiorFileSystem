package com.ss.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUploadFile() throws Exception {
        File file = new File("E:\\data\\develop\\efs素材\\起风了抖音配乐_爱给网_aigei_com.wav");
        FileInputStream fileInputStream = new FileInputStream(file);
           MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileInputStream);
        mockMvc.perform(
                MockMvcRequestBuilders.multipart("/upload")
                        .file(mockMultipartFile)
                        .param("bucketName","test")
        )
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
