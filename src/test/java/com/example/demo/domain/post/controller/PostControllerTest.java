package com.example.demo.domain.post.controller;

import com.example.demo.base.ControllerTest;
import com.example.demo.domain.auth.domain.UserPrincipal;
import com.example.demo.domain.file.domain.FileNameInfo;
import com.example.demo.domain.post.domain.page.PageInfo;
import com.example.demo.domain.post.domain.page.PageSort;
import com.example.demo.domain.post.domain.page.PageTitleInfo;
import com.example.demo.domain.post.domain.request.PostRequest;
import com.example.demo.domain.post.domain.response.PostInfoResponse;
import com.example.demo.domain.post.domain.response.PostPageResponse;
import com.example.demo.domain.post.service.PostService;
import com.example.demo.domain.user.domain.User;
import com.example.demo.domain.user.domain.vo.Track;
import com.example.demo.global.base.exception.ErrorCode;
import com.example.demo.global.base.exception.ServiceException;
import jdk.jfr.ContentType;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PostController.class)
@AutoConfigureRestDocs
@ExtendWith(MockitoExtension.class)
@WithMockUser
public class PostControllerTest {
    @InjectMocks
    private PostController postController;

    @MockBean
    private PostService postService;

    @Autowired
    private MockMvc mockMvc;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss");

    // UserPrincipal 모의 객체를 생성하는 메소드
    public static RequestPostProcessor userPrincipal() {
        User user = User.builder()
                .id(1L)
                .name("테스트")
                .email("test@kumoh.ac.kr")
                .track(Track.BACK)
                .major("컴퓨터공학과")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return mockHttpServletRequest -> {
            UserPrincipal mockUserPrincipal = new UserPrincipal(user);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(mockUserPrincipal, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
            return mockHttpServletRequest;
        };
    }


    @Nested
    @DisplayName("<게시판 저장>")
    class savePost {
        String savePostUrl = "/api/post/save";




        @Test
        @DisplayName("성공")
        void success() throws Exception {
            //Given
            PostRequest postRequest = postRequest();
            PostInfoResponse postInfoResponse = postInfoResponse();
            given(postService.postSave(any(PostRequest.class), any(Long.class))).willReturn(postInfoResponse);
            //When -> then
            mockMvc.perform(
                    MockMvcRequestBuilders
                            .multipart(savePostUrl)
                            .file((MockMultipartFile) postRequest.getAttachFile())
                            .file((MockMultipartFile) postRequest.getImageFiles().get(0))
                            .file((MockMultipartFile) postRequest.getImageFiles().get(1))
                            .param("title", postRequest.getTitle())
                            .param("contents", postRequest.getContents())
                            .param("track", String.valueOf(postRequest.getTrack()))

                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .with(csrf())
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(userPrincipal())
            ).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(postInfoResponse.getPostId()))
                    .andExpect(jsonPath("$.username").value(postInfoResponse.getUsername()))
                    .andExpect(jsonPath("$.title").value(postInfoResponse.getTitle()))
                    .andExpect(jsonPath("$.contents").value(postInfoResponse.getContents()))

                    .andExpect(jsonPath("$.attachFileNameInfo.originalFileName").value(postInfoResponse.getAttachFileNameInfo().getOriginalFileName()))
                    .andExpect(jsonPath("$.attachFileNameInfo.storeFileName").value(postInfoResponse.getAttachFileNameInfo().getStoreFileName()))
                    .andExpect(jsonPath("$.attachFileNameInfo.url").value(postInfoResponse.getAttachFileNameInfo().getUrl()))


                    .andExpect(jsonPath("$.imageFileNameInfos[0].originalFileName").value(postInfoResponse.getImageFileNameInfos().get(0).getOriginalFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[0].storeFileName").value(postInfoResponse.getImageFileNameInfos().get(0).getStoreFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[0].url").value(postInfoResponse.getImageFileNameInfos().get(0).getUrl()))

                    .andExpect(jsonPath("$.imageFileNameInfos[1].originalFileName").value(postInfoResponse.getImageFileNameInfos().get(1).getOriginalFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[1].storeFileName").value(postInfoResponse.getImageFileNameInfos().get(1).getStoreFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[1].url").value(postInfoResponse.getImageFileNameInfos().get(1).getUrl()))

                    .andExpect(jsonPath("$.createdAt").value(postInfoResponse.getUpdatedAt().format(formatter)))
                    .andExpect(jsonPath("$.updatedAt").value(postInfoResponse.getCreatedAt().format(formatter)))

                    .andDo(document("post/post-save-success",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestHeaders(
                            headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                    ),

                    requestParts(

                            partWithName("attachFile").description("첨부파일"),
                            partWithName("imageFiles").description("이미지 파일들")

                    ),



                    responseFields(
                            fieldWithPath("postId").type(JsonFieldType.NUMBER).description("게시물 ID"),
                            fieldWithPath("username").type(JsonFieldType.STRING).description("사용자 이름"),
                            fieldWithPath("title").type(JsonFieldType.STRING).description("게시물 제목"),
                            fieldWithPath("contents").type(JsonFieldType.STRING).description("게시물 내용"),
                            subsectionWithPath("attachFileNameInfo").type(JsonFieldType.OBJECT).description("첨부 파일 정보"),
                            fieldWithPath("attachFileNameInfo.originalFileName").type(JsonFieldType.STRING).description("원본 파일명"),
                            fieldWithPath("attachFileNameInfo.storeFileName").type(JsonFieldType.STRING).description("저장된 파일명"),
                            fieldWithPath("attachFileNameInfo.url").type(JsonFieldType.STRING).description("파일 URL"),
                            subsectionWithPath("imageFileNameInfos").type(JsonFieldType.ARRAY).description("이미지 파일 정보 리스트"),
                            fieldWithPath("imageFileNameInfos[].originalFileName").type(JsonFieldType.STRING).description("원본 이미지 파일명"),
                            fieldWithPath("imageFileNameInfos[].storeFileName").type(JsonFieldType.STRING).description("저장된 이미지 파일명"),
                            fieldWithPath("imageFileNameInfos[].url").type(JsonFieldType.STRING).description("이미지 파일 URL"),
                            fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 날짜"),
                            fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 날짜")
                    )));
        }




    }

    @Nested
    @DisplayName("<게시판 수정>")
    class updatePost {
        @Test
        @DisplayName("성공")
        void success() throws Exception {
            String updatePostUrl = "/api/post/update/{postId}";
            //Given
            PostRequest postRequest = postRequest();
            PostInfoResponse postInfoResponse = postInfoResponse();
            given(postService.postUpdate(any(PostRequest.class), any(String.class), any(Long.class))).willReturn(postInfoResponse);
            //When -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .multipart(updatePostUrl, "1")
                                    .file((MockMultipartFile) postRequest.getAttachFile())
                                    .file((MockMultipartFile) postRequest.getImageFiles().get(0))
                                    .file((MockMultipartFile) postRequest.getImageFiles().get(1))
                                    .param("title", postRequest.getTitle())
                                    .param("contents", postRequest.getContents())
                                    .param("track", String.valueOf(postRequest.getTrack()))
                                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(postInfoResponse.getPostId()))
                    .andExpect(jsonPath("$.username").value(postInfoResponse.getUsername()))
                    .andExpect(jsonPath("$.title").value(postInfoResponse.getTitle()))
                    .andExpect(jsonPath("$.contents").value(postInfoResponse.getContents()))

                    .andExpect(jsonPath("$.attachFileNameInfo.originalFileName").value(postInfoResponse.getAttachFileNameInfo().getOriginalFileName()))
                    .andExpect(jsonPath("$.attachFileNameInfo.storeFileName").value(postInfoResponse.getAttachFileNameInfo().getStoreFileName()))
                    .andExpect(jsonPath("$.attachFileNameInfo.url").value(postInfoResponse.getAttachFileNameInfo().getUrl()))


                    .andExpect(jsonPath("$.imageFileNameInfos[0].originalFileName").value(postInfoResponse.getImageFileNameInfos().get(0).getOriginalFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[0].storeFileName").value(postInfoResponse.getImageFileNameInfos().get(0).getStoreFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[0].url").value(postInfoResponse.getImageFileNameInfos().get(0).getUrl()))

                    .andExpect(jsonPath("$.imageFileNameInfos[1].originalFileName").value(postInfoResponse.getImageFileNameInfos().get(1).getOriginalFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[1].storeFileName").value(postInfoResponse.getImageFileNameInfos().get(1).getStoreFileName()))
                    .andExpect(jsonPath("$.imageFileNameInfos[1].url").value(postInfoResponse.getImageFileNameInfos().get(1).getUrl()))

                    .andExpect(jsonPath("$.createdAt").value(postInfoResponse.getUpdatedAt().format(formatter)))
                    .andExpect(jsonPath("$.updatedAt").value(postInfoResponse.getCreatedAt().format(formatter)))

                    .andDo(document("post/post-update-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            ),

                            requestParts(

                                    partWithName("attachFile").description("첨부파일"),
                                    partWithName("imageFiles").description("이미지 파일들")

                            ),


                            responseFields(
                                    fieldWithPath("postId").type(JsonFieldType.NUMBER).description("게시물 ID"),
                                    fieldWithPath("username").type(JsonFieldType.STRING).description("사용자 이름"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("수정된 게시물 제목"),
                                    fieldWithPath("contents").type(JsonFieldType.STRING).description("수정된 게시물 내용"),
                                    subsectionWithPath("attachFileNameInfo").type(JsonFieldType.OBJECT).description("수정된 첨부 파일 정보"),
                                    fieldWithPath("attachFileNameInfo.originalFileName").type(JsonFieldType.STRING).description("수정된 원본 파일명"),
                                    fieldWithPath("attachFileNameInfo.storeFileName").type(JsonFieldType.STRING).description("수정된 저장된 파일명"),
                                    fieldWithPath("attachFileNameInfo.url").type(JsonFieldType.STRING).description("수정된 파일 URL"),
                                    subsectionWithPath("imageFileNameInfos").type(JsonFieldType.ARRAY).description("수정된 이미지 파일 정보 리스트"),
                                    fieldWithPath("imageFileNameInfos[].originalFileName").type(JsonFieldType.STRING).description("수정된 원본 이미지 파일명"),
                                    fieldWithPath("imageFileNameInfos[].storeFileName").type(JsonFieldType.STRING).description("수정된 저장된 이미지 파일명"),
                                    fieldWithPath("imageFileNameInfos[].url").type(JsonFieldType.STRING).description("수정된 이미지 파일 URL"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 날짜"),
                                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 날짜")
                            )));
        }

        @Test
        @DisplayName("user의 게시물이 아닌 경우울 에러 반환")
        void FailNotMathcedUser() throws Exception {
            String updatePostUrl = "/api/post/update/{postId}";
            //Given
            PostRequest postRequest = postRequest();
            PostInfoResponse postInfoResponse = postInfoResponse();
            given(postService.postUpdate(any(PostRequest.class), any(String.class), any(Long.class))).willThrow(new ServiceException(ErrorCode.NOT_ACCESS_USER));

            //When -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .multipart(updatePostUrl, "1")
                                    .file((MockMultipartFile) postRequest.getAttachFile())
                                    .file((MockMultipartFile) postRequest.getImageFiles().get(0))
                                    .file((MockMultipartFile) postRequest.getImageFiles().get(1))
                                    .param("title", postRequest.getTitle())
                                    .param("contents", postRequest.getContents())
                                    .param("track", String.valueOf(postRequest.getTrack()))
                                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                    .andExpect(status().isUnauthorized())


                    .andDo(document("post/post-update-not-matched-user",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            ),
                            responseFields(
                                    fieldWithPath("timestamp").description("에러 발생 시간"),
                                    fieldWithPath("message").description("에러 메시지")
                            )));
        }
        @Test
        @DisplayName("게시물을 찾을 수 없으면 에러 반환")
        void FailPostNotFound() throws Exception {
            String updatePostUrl = "/api/post/update/{postId}";
            //Given
            PostRequest postRequest = postRequest();
            given(postService.postUpdate(any(PostRequest.class), any(String.class), any(Long.class))).willThrow(new ServiceException(ErrorCode.POST_NOT_FOUND));

            //When -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .multipart(updatePostUrl, "1")
                                    .file((MockMultipartFile) postRequest.getAttachFile())
                                    .file((MockMultipartFile) postRequest.getImageFiles().get(0))
                                    .file((MockMultipartFile) postRequest.getImageFiles().get(1))
                                    .param("title", postRequest.getTitle())
                                    .param("contents", postRequest.getContents())
                                    .param("track", String.valueOf(postRequest.getTrack()))
                                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                    .andExpect(status().isNotFound())


                    .andDo(document("post/post-update-not-found-post",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            ),
                            responseFields(
                                    fieldWithPath("timestamp").description("에러 발생 시간"),
                                    fieldWithPath("message").description("에러 메시지")
                            )));
        }
    }

    @Nested
    @DisplayName("<게시물 삭제>")
    class deletePost {
        String deletePostUrl = "/api/post/delete/{postId}";
        @Test
        @DisplayName("성공")
        void success() throws Exception {
            //Given
            PostRequest postRequest = postRequest();
            PostInfoResponse postInfoResponse = postInfoResponse();
            doNothing().when(postService).postRemove(any(Long.class),any(String.class));
            //When -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .patch(deletePostUrl, "1")

                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                    .andExpect(status().isOk())


                    .andDo(document("post/post-delete-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            )));
        }
        @Test
        @DisplayName("user의 게시물이 아닌 경우울 에러 반환")
        void FailNotMatchedUser() throws Exception {

            //Given
            doThrow(new ServiceException(ErrorCode.NOT_ACCESS_USER))
                    .when(postService).postRemove(any(Long.class),any(String.class));

            //When -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .patch(deletePostUrl, "1")
                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                    .andExpect(status().isUnauthorized())


                    .andDo(document("post/post-delete-not-matched-user",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            ),
                            responseFields(
                                    fieldWithPath("timestamp").description("에러 발생 시간"),
                                    fieldWithPath("message").description("에러 메시지")
                            )));
        }
        @Test
        @DisplayName("게시물을 찾을 수 없으면 에러 반환")
        void FailPostNotFound() throws Exception {
            //Given
            doThrow(new ServiceException(ErrorCode.POST_NOT_FOUND))
                    .when(postService).postRemove(any(Long.class),any(String.class));


            //When -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .patch(deletePostUrl, "1")
                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                    .andExpect(status().isNotFound())


                    .andDo(document("post/post-delete-not-found-post",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            ),
                            responseFields(
                                    fieldWithPath("timestamp").description("에러 발생 시간"),
                                    fieldWithPath("message").description("에러 메시지")
                            )));
        }
    }

    @Nested
    @DisplayName("<단일 게시물 검색>")
    class searchPost {
        String searchPostUrl = "/api/post/search/{postId}";
        @Test
        @DisplayName("성공")
        void success() throws Exception {
            //Given
            PostInfoResponse postInfoResponse = postInfoResponse();
            given(postService.findById(any(Long.class),any(String.class))).willReturn(postInfoResponse);

            //when -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get(searchPostUrl, 1)
                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.postId").value(postInfoResponse.getPostId()))
                        .andExpect(jsonPath("$.username").value(postInfoResponse.getUsername()))
                        .andExpect(jsonPath("$.title").value(postInfoResponse.getTitle()))
                        .andExpect(jsonPath("$.contents").value(postInfoResponse.getContents()))

                        .andExpect(jsonPath("$.attachFileNameInfo.originalFileName").value(postInfoResponse.getAttachFileNameInfo().getOriginalFileName()))
                        .andExpect(jsonPath("$.attachFileNameInfo.storeFileName").value(postInfoResponse.getAttachFileNameInfo().getStoreFileName()))
                        .andExpect(jsonPath("$.attachFileNameInfo.url").value(postInfoResponse.getAttachFileNameInfo().getUrl()))


                        .andExpect(jsonPath("$.imageFileNameInfos[0].originalFileName").value(postInfoResponse.getImageFileNameInfos().get(0).getOriginalFileName()))
                        .andExpect(jsonPath("$.imageFileNameInfos[0].storeFileName").value(postInfoResponse.getImageFileNameInfos().get(0).getStoreFileName()))
                        .andExpect(jsonPath("$.imageFileNameInfos[0].url").value(postInfoResponse.getImageFileNameInfos().get(0).getUrl()))

                        .andExpect(jsonPath("$.imageFileNameInfos[1].originalFileName").value(postInfoResponse.getImageFileNameInfos().get(1).getOriginalFileName()))
                        .andExpect(jsonPath("$.imageFileNameInfos[1].storeFileName").value(postInfoResponse.getImageFileNameInfos().get(1).getStoreFileName()))
                        .andExpect(jsonPath("$.imageFileNameInfos[1].url").value(postInfoResponse.getImageFileNameInfos().get(1).getUrl()))

                        .andExpect(jsonPath("$.createdAt").value(postInfoResponse.getUpdatedAt().format(formatter)))
                        .andExpect(jsonPath("$.updatedAt").value(postInfoResponse.getCreatedAt().format(formatter)))

                    .andDo(document("post/post-search-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            ),

                            responseFields(
                                    fieldWithPath("postId").type(JsonFieldType.NUMBER).description("게시물 ID"),
                                    fieldWithPath("username").type(JsonFieldType.STRING).description("사용자 이름"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("게시물 제목"),
                                    fieldWithPath("contents").type(JsonFieldType.STRING).description("게시물 내용"),
                                    subsectionWithPath("attachFileNameInfo").type(JsonFieldType.OBJECT).description("첨부 파일 정보"),
                                    fieldWithPath("attachFileNameInfo.originalFileName").type(JsonFieldType.STRING).description("원본 파일명"),
                                    fieldWithPath("attachFileNameInfo.storeFileName").type(JsonFieldType.STRING).description("저장된 파일명"),
                                    fieldWithPath("attachFileNameInfo.url").type(JsonFieldType.STRING).description("파일 URL"),
                                    subsectionWithPath("imageFileNameInfos").type(JsonFieldType.ARRAY).description("이미지 파일 정보 리스트"),
                                    fieldWithPath("imageFileNameInfos[].originalFileName").type(JsonFieldType.STRING).description("원본 이미지 파일명"),
                                    fieldWithPath("imageFileNameInfos[].storeFileName").type(JsonFieldType.STRING).description("저장된 이미지 파일명"),
                                    fieldWithPath("imageFileNameInfos[].url").type(JsonFieldType.STRING).description("이미지 파일 URL"),
                                    fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 날짜"),
                                    fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("수정 날짜")
                            )));
        }
        @Test
        @DisplayName("게시물을 찾을 수 없으면 에러 반환")
        void failIfPostNotFound() throws Exception {
            //given
            given(postService.findById( any(Long.class),any(String.class))).willThrow(new ServiceException(ErrorCode.POST_NOT_FOUND));

            //When -> then
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get(searchPostUrl, "1")
                                    .with(csrf())
                                    .header("Authorization", "Bearer {ACCESS_TOKEN}")
                                    .with(userPrincipal())
                    ).andDo(print())
                    .andExpect(status().isNotFound())

                    .andDo(document("post/post-search-not-found-post",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer 토큰을 포함한 인증 헤더")
                            ),
                            responseFields(
                                    fieldWithPath("timestamp").description("에러 발생 시간"),
                                    fieldWithPath("message").description("에러 메시지")
                            )));
        }
    }












    @Nested
    @DisplayName("<페이징 조회>")
    class pagging {
        String pagelistUrl = "/api/post/list";
        @Test
        @DisplayName("성공")
        void success() throws Exception {
            //given
            PostPageResponse postPageResponse = createTestPostPageResponse();
            given(postService.findPageList(any(Integer.class), any(Track.class), any(PageSort.class)))
                    .willReturn(postPageResponse);

            //when -> then
            mockMvc
                    .perform(
                            MockMvcRequestBuilders
                                    .get(pagelistUrl)
                                    .queryParam("page", "1")
                                    .queryParam("track", "BACK")
                                    .queryParam("pageSort", "DESC")

                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageTitleInfoList[0].postId").value(postPageResponse.getPageTitleInfoList().get(0).getPostId()))
                    .andExpect(jsonPath("$.pageTitleInfoList[0].userName").value(postPageResponse.getPageTitleInfoList().get(0).getUserName()))
                    .andExpect(jsonPath("$.pageTitleInfoList[0].title").value(postPageResponse.getPageTitleInfoList().get(0).getTitle()))
                    .andExpect(jsonPath("$.pageTitleInfoList[0].track").value(postPageResponse.getPageTitleInfoList().get(0).getTrack().toString()))
                    .andExpect(jsonPath("$.pageTitleInfoList[0].createdAt").value(postPageResponse.getPageTitleInfoList().get(0).getCreatedAt().format(formatter)))
                    // 검증 추가
                    .andExpect(jsonPath("$.pageTitleInfoList[1].postId").value(postPageResponse.getPageTitleInfoList().get(1).getPostId()))
                    .andExpect(jsonPath("$.pageTitleInfoList[1].userName").value(postPageResponse.getPageTitleInfoList().get(1).getUserName()))
                    .andExpect(jsonPath("$.pageTitleInfoList[1].title").value(postPageResponse.getPageTitleInfoList().get(1).getTitle()))
                    .andExpect(jsonPath("$.pageTitleInfoList[1].track").value(postPageResponse.getPageTitleInfoList().get(1).getTrack().toString()))
                    .andExpect(jsonPath("$.pageTitleInfoList[1].createdAt").value(postPageResponse.getPageTitleInfoList().get(1).getCreatedAt().format(formatter)))
                    .andExpect(jsonPath("$.pageInfo.pageSize").value(postPageResponse.getPageInfo().getPageSize()))
                    .andExpect(jsonPath("$.pageInfo.page").value(postPageResponse.getPageInfo().getPage()))
                    .andExpect(jsonPath("$.pageInfo.totalPage").value(postPageResponse.getPageInfo().getTotalPage()))
                    .andExpect(jsonPath("$.pageInfo.pageSort").value(postPageResponse.getPageInfo().getPageSort().toString()))

                    .andDo(document("post/post-page-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("page").description("페이지"),
                                    parameterWithName("track").description("게시물"),
                                    parameterWithName("pageSort").description("페이징 정렬")
                            ),
                            responseFields(
                                    fieldWithPath("pageTitleInfoList").type(JsonFieldType.ARRAY).description("페이지에 게시물 정보 리스트"),
                                    fieldWithPath("pageTitleInfoList[].postId").type(JsonFieldType.NUMBER).description("게시물 ID"),
                                    fieldWithPath("pageTitleInfoList[].title").type(JsonFieldType.STRING).description("게시물 제목"),
                                    fieldWithPath("pageTitleInfoList[].userName").type(JsonFieldType.STRING).description("사용자 이름"),
                                    fieldWithPath("pageTitleInfoList[].track").type(JsonFieldType.STRING).description("트랙"),
                                    fieldWithPath("pageTitleInfoList[].createdAt").type(JsonFieldType.STRING).description("작성 시간"),
                                    fieldWithPath("pageInfo").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                    fieldWithPath("pageInfo.pageSize").type(JsonFieldType.NUMBER).description("한 페이지당 게시물 수"),
                                    fieldWithPath("pageInfo.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("pageInfo.totalPage").type(JsonFieldType.NUMBER).description("총 페이지 수"),
                                    fieldWithPath("pageInfo.pageSort").type(JsonFieldType.STRING).description("정렬 방식")
                            )));
        }
        @Test
        @DisplayName("쿼리 타입 미스매치")
        void fail() throws Exception {
            //given

            //when -> then
            mockMvc
                    .perform(
                            MockMvcRequestBuilders
                                    .get(pagelistUrl)
                                    .queryParam("page", "fdsf")
                                    .queryParam("pageSort", "DESC")
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andDo(document("post/post-page-query-mismatch",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),

                            responseFields(
                                    fieldWithPath("timestamp").description("에러 발생 시간"),
                                    fieldWithPath("message").description("에러 메시지")
                            )));

        };
        @Test
        @DisplayName("필수 인자 제외")
        void fail2() throws Exception {
            //given

            //when -> then
            mockMvc
                    .perform(
                            MockMvcRequestBuilders
                                    .get(pagelistUrl)
                                    .queryParam("page", "1")
                                    .queryParam("pageSort", "DESC")
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andDo(document("post/post-page-query-required",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),

                            responseFields(
                                    fieldWithPath("timestamp").description("에러 발생 시간"),
                                    fieldWithPath("message").description("에러 메시지")
                            )));

        };

    }














    PostRequest postRequest() {
        MultipartFile attachFile = new MockMultipartFile("attachFile", "첨부파일1.pdf", MediaType.APPLICATION_PDF_VALUE, "attachFile".getBytes());
        List<MultipartFile> imageFiles = List.of(
                new MockMultipartFile("imageFiles", "이미지파일1.PNG", MediaType.IMAGE_PNG_VALUE, "test1".getBytes()),
                new MockMultipartFile("imageFiles", "이미지파일2.PNG", MediaType.IMAGE_PNG_VALUE, "test2".getBytes())
        );
        return PostRequest.builder()
                .title("title")
                .contents("content")
                .track(Track.BACK)
                .attachFile(attachFile)
                .imageFiles(imageFiles)
                .build();
    }
    PostInfoResponse postInfoResponse() {
        // FileNameInfo 객체 예시 생성
        FileNameInfo attachFile = new FileNameInfo("originalFileName.pdf", "storeFileName20230315.pdf", "https://example.com/files/storeFileName20230315.pdf");
        List<FileNameInfo> imageFiles = Arrays.asList(
                new FileNameInfo("image1.jpg", "storeImage120230315.jpg", "https://example.com/images/storeImage120230315.jpg"),
                new FileNameInfo("image2.jpg", "storeImage220230315.jpg", "https://example.com/images/storeImage220230315.jpg")
        );
        return PostInfoResponse.builder()
                .postId(1L)
                .title("title")
                .contents("content")
                .username("username")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .attachFileNameInfo(attachFile)
                .imageFileNameInfos(imageFiles)
                .build();
    }
    PostPageResponse createTestPostPageResponse() {
        List<PageTitleInfo> pageTitleInfoList = new ArrayList<>();
        pageTitleInfoList.add(PageTitleInfo.builder()
                .postId(1L)
                .title("첫 번째 게시글")
                .userName("홍길동")
                .track(Track.BACK)
                .createdAt(LocalDateTime.now())
                .build());
        pageTitleInfoList.add(PageTitleInfo.builder()
                .postId(2L)
                .title("두 번째 게시글")
                .userName("김서방")
                .track(Track.FRONT)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());
        PageInfo pageInfo = new PageInfo(10, 1, 2, PageSort.DESC);

        return new PostPageResponse(pageTitleInfoList, pageInfo);
    }
}