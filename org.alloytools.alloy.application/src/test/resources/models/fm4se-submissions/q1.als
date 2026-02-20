/* Create an Alloy model for a scenario of your choice. 
* Declare at least 4 signatures each with at least 2 
  fields. 
* Use inheritance between signatures at least once.
* Define at least 2 facts and 2 predicates.
* Add two run commands to your model.
  * The first run command should be unsatisfiable.
  * The second run command should be satisfiable 
    and return at least 2 instances.
*/

-- signatures

abstract sig Person {}
sig Coach, Player in Person {}

some sig Team {
    coach: one Coach,
    players: some Player
}

sig Fixture {
    team1: one Team,
    team2: one Team,
}

sig Result {
    related_fixture: one Fixture,
    winner: one Team,
}

sig Award {
    -- each result provides an award to a player
    related_result: one Result,
    player_awarded: one Player,
}

-- facts

fact {
    -- Team: two teams cannot have the same coach
    all disj t1, t2: Team | 
        	t1.coach != t2.coach
}
fact {    
    -- Team: two teams cannot have same player in squad
    all disj t1, t2: Team, p: Player 
        | not ((p in t1.players) && (p in t2.players))
}
fact {
    -- Fixture: both teams cant be same
    all disj f: Fixture 
        | f.team1 != f.team2
}
fact {
    -- two fixtures cant be same
    all disj f1, f2: Fixture 
        | not 
            ((f1.team1 = f2.team1 and f1.team2 = f2.team2) 
                or (f1.team1 = f2.team2 and f1.team2 = f2.team1))
}
fact {
    -- Result: two results cant have same fixture
    all disj r1, r2: Result
        |  r1.related_fixture != r2.related_fixture
}
fact {
    -- winner must be from teams in rel fixture
    all disj r: Result 
        | r.winner = r.related_fixture.team1 
            or r.winner = r.related_fixture.team2 
}
fact {  
    -- Award: two awards cannot relate to same result 
    all disj a1, a2: Award 
        | a1.related_result != a2.related_result
}

fact {
    -- Award: only player from winning team gets award
    all disj a: Award
        | a.player_awarded in a.related_result.winner.players
}

fact {
    all disj t: Team |
        some f: Fixture | t in f.team1 or t in f.team2
}

fact {
    all disj f: Fixture |
        some r: Result | f in r.related_fixture
}

-- predicates

-- 1: all players get award
pred AllPlayersGetAward {
    all p: Player |
        some a: Award | p in a.player_awarded
}

-- 2: no team wins
pred NoTeamWins {
    all r: Result, t: Team |
        t not in r.winner
}

run NoTeamWins
run AllPlayersGetAward