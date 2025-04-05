package moe.ku6.sekaimagic.command.impl;

import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.data.SekaiDatabase;
import org.jline.jansi.Ansi;

@Slf4j
public class SearchCommand implements ICommand<SearchCommand.Params> {
    @Override
    public String[] GetNames() {
        return new String[] {"s", "search"};
    }

    @Override
    public String GetManual() {
        return "Search for a package by its title or pronounciation.";
    }

    @Override
    public void HandleInternal(Params args) throws Exception {
        var candidates = SekaiDatabase.getInstance().getPackages().values().stream()
                .filter(c -> c.getTitle().toLowerCase().contains(args.keyword.toLowerCase()) || c.getPronounciation().toLowerCase().matches(args.keyword.toLowerCase()))
                .limit(args.limit > 0 ? args.limit : Long.MAX_VALUE)
                .toList();

        log.info("Search results:");

        for (var candidate : candidates) {
            log.info(candidate.GetInfoLine());
        }

        log.info(Ansi.ansi().a("({} total) ").fgGreen().a("Use `info <id>` to see details about a package").reset().toString(), candidates.size());
    }

    public static class Params {
        @Parameter(description = "The keyword to search for. Can be either title or pronounciation.", required = true)
        public String keyword;

        @Parameter(
                names = {"-l", "--limit"},
                description = "The maximum number of results to return. Pass 0 or less to disable limit."
        )
        public int limit = 10;
    }
}
