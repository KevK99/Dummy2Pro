/** package me.daskabel.dummy2pro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.daskabel.dummy2pro.model.UserQuestionProgress;
import me.daskabel.dummy2pro.model.UserQuestionProgressId;

/**
 * Datenbankzugriff für den Lernfortschritt eines Users.
 *
 * Speichert pro User + Frage: Status (OPEN / CORRECT / WRONG), die gewählte
 * Antwort und den Zeitstempel.
 */
public interface UserQuestionProgressRepository
			extends JpaRepository<UserQuestionProgress, UserQuestionProgressId>
{

	/**
	 * Alle Fortschrittseinträge eines Users (für Dashboard-Übersicht).
	 */
	List<UserQuestionProgress> findByUserUserId(Long userId);

	/**
	 * Einen einzelnen Fortschrittseintrag für User + Frage.
	 */
	@Query("""
				    SELECT uqp FROM UserQuestionProgress uqp
				    WHERE uqp.user.userId = :userId
				      AND uqp.question.questionId = :questionId
				""")
	Optional<UserQuestionProgress> findByUserIdAndQuestionId(@Param("userId") Long userId,
				@Param("questionId") Long questionId);

	/**
	 * Alle Fortschrittseinträge eines Users für Fragen eines bestimmten Themes.
	 * Wird genutzt, um den Raum-Status (%, Bronze/Silber/Gold) zu berechnen.
	 */
	@Query("""
				    SELECT uqp FROM UserQuestionProgress uqp
				    JOIN uqp.question q
				    JOIN q.themes t
				    WHERE uqp.user.userId = :userId
				      AND t.themeId = :themeId
				""")
	List<UserQuestionProgress> findByUserIdAndThemeId(@Param("userId") Long userId,
				@Param("themeId") Long themeId);
}