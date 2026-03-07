package me.daskabel.dummy2pro.repository;

import me.daskabel.dummy2pro.model.QuestionProgress;
import me.daskabel.dummy2pro.model.QuestionProgressId;
import me.daskabel.dummy2pro.model.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionProgressRepository extends JpaRepository<QuestionProgress, QuestionProgressId> {

    List<QuestionProgress> findByRun_RunId(Long runId);

    List<QuestionProgress> findByRun_RunIdOrderByQuestion_QuestionIdAsc(Long runId);

    Optional<QuestionProgress> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    long countByRun_RunId(Long runId);

    long countByRun_RunIdAndStatus(Long runId, ProgressStatus status);
}