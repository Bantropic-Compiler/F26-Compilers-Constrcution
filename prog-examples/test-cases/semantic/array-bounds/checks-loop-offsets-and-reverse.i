routine p(a: array [5] integer) is
    for i in 1..5 reverse loop
        print a[i]
        print a[i + 1]
        print a[i - 1]
    end
end
