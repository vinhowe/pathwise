# Pathwise

<img alt="maze demo" src="./screenshots/maze-astar.png" width="300"/>

WIP pathfinding library for Bukkit plugins.

Thanks to Amit Patel and his unusual knack for explaining things clearly. My working A\* implementation is based heavily on [Introduction to A\*](https://www.redblobgames.com/pathfinding/a-star/introduction.html) and [Implementation of A\*](https://www.redblobgames.com/pathfinding/a-star/implementation.html).

## TODO

- [ ] Implement [Incremental Phi\*](http://www.cs.cmu.edu/~maxim/files/inctheta_ijcai09.pdf)
- [ ] Improve neighbor evaluation
  - [ ] Fix corner cutting
  - [ ] No going straight up unless flag is set on some level that allows flying--require that block below target isn't passable
    - [ ] Except ladders, vines, water from this rule
  - [ ] Figure out how to deal with block height and prevent pathfinding over fences etc.
  - [ ] Prevent pathfinding through lava or over whatever other blocks hurt players (magma)
- [ ] Improve cost function
  - [ ] Assign a small cost for jumping up from one block to another, just because it's annoying
  - [ ] Avoid water
    - [ ] This cost could compound to account for running out of breath
    - [ ] For soul sand bubbles: lower cost when going up, raise cost when going down
  - [ ] Assign cost for any "slow" blocks like soul sand
- [ ] Look into adding a Y arc for paths where flying makes sense and is available
