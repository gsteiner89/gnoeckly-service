package at.stoneforge.gnoeckly.ranking;

import java.util.UUID;

/** Interface-Projektion fuer {@code JokeRepository.rankAuthors*} (Aliasnamen = Getter). */
public interface AuthorRankingRow {

    UUID getAuthorId();

    long getKarma();

    long getApprovedJokes();
}
