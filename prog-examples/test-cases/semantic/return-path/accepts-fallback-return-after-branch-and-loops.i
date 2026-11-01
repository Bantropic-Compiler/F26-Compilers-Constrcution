routine f(b: boolean): integer is
    if b then
        return 1
    end
    while b loop
        break
        return 2
    end
    for i in 1..3 loop
        continue
        return 3
    end
    return 4
end
