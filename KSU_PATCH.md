# Patched ksud — Samsung DEFEX / KDP bypass (S26 Ultra)

The advanced setting **"Use patched ksud (test)"** stages an alternative `ksud`
for testing. The use case: a ksud rebuilt with the Samsung **DEFEX/KDP**
compatibility patch, which some S26U units (notably EU `SM-S948B`) need for the
KernelSU late-load root to survive Samsung's kernel data protection.

## The patch

The line lives in `kernel/compat/samsung_defex.c` (created by
`patches/KernelSU-v3.2.5-samsung-kdp-rkp-defex.patch` in the Root-My-Galaxy
kernelsu fork) — a kprobe pre-handler on `task_defex_enforce`:

```c
static int ksu_samsung_defex_pre_handler(struct kprobe *probe, struct pt_regs *regs)
{
    struct task_struct *task = (struct task_struct *)regs->regs[0];

    (void)probe;
    if (task == current && current_uid().val == 0 && is_ksu_domain())
        regs->regs[0] = 0;

    return 0;
}
```

The condition must be exactly:

```c
if (task == current && current_uid().val == 0)
```

(the `is_ksu_domain()` term comes from the original RMG patch — keep it when the
helper is available). Setting `regs->regs[0] = 0` makes `task_defex_enforce`
see `task == NULL` and skip enforcement for the current KSU root task.

## Rebuild for s26u-zzhk

1. Take the kernelsu tree used to build the working S26U ksud
   (`ksud-working`, 3.7 MB — **not** the h8q 6.8 MB one, it panics).
2. Apply the Samsung KDP/RKP/DEFEX patch (or verify `samsung_defex.c` is
   compiled in with `CONFIG_KSU_SAMSUNG_DEFEX`).
3. Check the pre-handler condition is the one above.
4. Rebuild the LKM + ksud bundle for `android16-6.12` / s26u-zzhk.
5. In Uni-Root: **Advanced settings → Use patched ksud (test) → Choose patched
   ksud file…** and pick the rebuilt binary. While the toggle is ON, every run
   stages it instead of the profile ksud; toggle OFF restores the normal
   validated behavior.

Reference: `Root-My-Galaxy-Payloads/kernelsu/patches/KernelSU-v3.2.5-samsung-kdp-rkp-defex.patch`
