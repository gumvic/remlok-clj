## TODO

### Enhancements

- Consider adding middleware. 
Registered for `:remlok/mware` topic, it will be `handler -> handler*`.
Update the docstrings and README.

- Consider multiple remotes. {:loc, :http-a, :http-b, :ws, ...}.
`send` should be topic-parametrized, too.
Update the docstrings.
Update README, kill the `locrem` thing.
Add an example featuring local storage.

- remote `pubf` and `mutf` should emit cross-platform warning that they don't do anything.

### Misc
 
- Add production `lein` options.

- There should be at least some tests.