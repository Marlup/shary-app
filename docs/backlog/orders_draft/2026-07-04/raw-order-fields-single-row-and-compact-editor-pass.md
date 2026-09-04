# Raw order backup

Apply the next UI correction pass with these requirements:

1. In `Fields`, remove the first top layer that contained the `Fields` text, search button, and vertical dots, and merge the controls into one shared layer.
2. Keep `Fields` text, but place search, overflow, cloud inbox, sort, and tags together in the same control layer.
3. Remove `Add field` from the top controls.
4. Remove the standalone `Cloud ready` status and make the cloud inbox control carry the minimal readiness information itself with red/green icon signaling and short text.
5. Put the `Add field` compact button at the bottom center, above the tabs and below the field list.
6. Rename `Field statistics` to `Fields info`.
7. In the add field form, move required guidance into suggestion text such as `(required) Enter key`.
8. Remove the typed-value toggle, default to `Category.Text` for simple text, and move `Value type` and `Tag` into the top horizontal row near the title as compact controls.
9. Remove the advanced-options expander and keep alias as `(Optional) Alias`.
10. Overall goal: more compact and intuitive UI with fewer explicit texts when icons and minimal labels already communicate the meaning.
