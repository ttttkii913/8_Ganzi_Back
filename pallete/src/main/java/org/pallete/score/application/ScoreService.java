package org.pallete.score.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pallete.common.BusinessException;
import org.pallete.diary.domain.Diary;
import org.pallete.diary.repository.DiaryRepository;
import org.pallete.login.model.User;
import org.pallete.login.repository.UserRepository;
import org.pallete.score.api.dto.request.ScoreSaveReqDto;
import org.pallete.score.api.dto.response.ScoreInfoResDto;
import org.pallete.score.api.dto.response.ScoreListResDto;
import org.pallete.score.domain.Score;
import org.pallete.score.domain.repository.ScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreService {
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;

    // 점수 생성 - 사용자가 해당 다이어리에 점수를 매길 때
    @Transactional
    public ScoreInfoResDto scoreSave(Long diaryId, ScoreSaveReqDto scoreSaveReqDto, HttpServletRequest request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 일기가 없습니다."));

        String userEmail = getEmailFromSession(request);
        User user = userRepository.findByEmail(userEmail);

        Score score = scoreSaveReqDto.toEntity(user, diary);
        scoreRepository.save(score);

        return ScoreInfoResDto.from(score);
    }

    // 사용자에 따라 점수 전체 조회
    public ScoreListResDto scoreFindAll(HttpServletRequest request) {
        String userEmail = getEmailFromSession(request);

        List<Score> userScores = scoreRepository.findAllByUserEmail(userEmail);

        List<ScoreInfoResDto> scoreInfoResDtoList = userScores.stream()
                .map(ScoreInfoResDto::from)
                .toList();

        return ScoreListResDto.from(scoreInfoResDtoList);
    }

    // 일기에 따라 점수 한 개만 조회 - 사용자의 특정 일기에 대한 점수
    public ScoreInfoResDto scoreFindOne(Long diaryId, HttpServletRequest request) {
        String userEmail = getEmailFromSession(request);

        // 사용자의 특정 일기에 대해 하나의 점수를 조회 (userId와 diaryId를 기준으로)
        Score score = scoreRepository.findByUserEmailAndDiaryId(userEmail, diaryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일기의 점수를 찾을 수 없습니다."));

        return ScoreInfoResDto.from(score);
    }

    // 세션에서 이메일 가져오기
    private String getEmailFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            throw new IllegalStateException("로그인 상태가 아닙니다.");
        }
        return (String) session.getAttribute("userEmail");
    }

}
