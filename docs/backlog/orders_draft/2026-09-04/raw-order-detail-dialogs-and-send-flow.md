# Raw order backup - detail-dialog metadata refinement + send-flow fix

## User order

> I want you to refine and adapt the logic which I included manually in
> FieldDetailDialog.kt, between lines ~ [105 - 140]. The intention was to
> include Added at date and tag in the same horizontal block to leverage space
> and not leave too much blank space. Include those in SharyModalScaffold.kt,
> which is the core composable.
>
> On the other hand, and in the same FieldDetailDialog, the labels 'Value'
> 'Alias', Added and Tag, won't contrast wrt to their specific values, i.e. the
> pairs are not distinguished properly. Suggest a way to differentiate both.
>
> Also, redo that in the same detail dialog for Contacts and Requests.
>
> The Contact Detail button 'Select Fields' works, but at after jumping to the
> fields screen and selecting any fields, The button to review data to be sent
> is not shown, but 'Select Users'. Therefore, the workflow is not adapted
> (Also change name User to Contact). Analyze and fix that.
>
> Also, the review button is a simple righted arrow. Aesthetically is ok, but it
> is not intuitive about what it does and where it goes. Analyze and suggest.

## Clarifying answers

- Label/value differentiation: container + label styling (value inside a light
  rounded surfaceVariant container; label smaller, semi-bold, muted).
- Review CTA label: "Review & send".
